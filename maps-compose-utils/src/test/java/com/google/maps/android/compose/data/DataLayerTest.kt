/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.maps.android.compose.data

import android.content.Context
import android.graphics.Bitmap
import com.google.android.gms.maps.GoogleMap
import com.google.maps.android.collections.GroundOverlayManager
import com.google.maps.android.collections.MarkerManager
import com.google.maps.android.collections.PolygonManager
import com.google.maps.android.collections.PolylineManager
import com.google.maps.android.data.DataLayerLoader
import com.google.maps.android.data.renderer.IconProvider
import com.google.maps.android.data.renderer.mapview.MapViewRenderer
import com.google.maps.android.data.renderer.model.DataLayer
import com.google.maps.android.data.renderer.model.DataScene
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
public class DataLayerTest {

    @Test
    public fun testRenderSceneWithCachedImagesCachesBitmapsBeforeRender() {
        val renderer = mockk<MapViewRenderer>(relaxed = true)
        val bitmap = mockk<Bitmap>()
        every { renderer.cacheImageData(any(), any()) } just runs
        every { renderer.render(any()) } just runs

        val layerWithImages = DataLayer(
            features = emptyList(),
            properties = mapOf(
                "images" to mapOf(
                    "icons/pin.png" to bitmap,
                    "icons/invalid.png" to "not-a-bitmap",
                    123 to bitmap,
                ),
            ),
        )
        val layerWithoutImages = DataLayer(
            features = emptyList(),
            properties = emptyMap(),
        )
        val scene = DataScene(listOf(layerWithImages, layerWithoutImages))

        renderSceneWithCachedImages(renderer, scene)

        verify(exactly = 1) { renderer.cacheImageData("icons/pin.png", bitmap) }
        verify { renderer.render(scene) }
    }

    @Test
    public fun testCreateAndRenderSceneSkipsReattachWhenNoManagersProvided() {
        val map = mockk<GoogleMap>(relaxed = true)
        val iconProvider = mockk<IconProvider>(relaxed = true)
        val renderer = mockk<MapViewRenderer>(relaxed = true)
        val scene = DataScene(emptyList())
        var reattached = false

        val created = createAndRenderScene(
            map = map,
            scene = scene,
            iconProvider = iconProvider,
            useAdvancedMarkers = true,
            markerManager = null,
            polygonManager = null,
            polylineManager = null,
            groundOverlayManager = null,
            reattach = { reattached = true },
            rendererFactory = { renderer },
        )

        assertSame(renderer, created)
        assertFalse(reattached)
        verify { renderer.useAdvancedMarkers = true }
        verify { renderer.render(scene) }
    }

    @Test
    public fun testCreateAndRenderSceneInvokesReattachWhenAnyManagerProvided() {
        val map = mockk<GoogleMap>(relaxed = true)
        val iconProvider = mockk<IconProvider>(relaxed = true)
        val scene = DataScene(emptyList())

        val managers = listOf(
            Quad(mockk<MarkerManager>(relaxed = true), null, null, null),
            Quad(null, mockk<PolygonManager>(relaxed = true), null, null),
            Quad(null, null, mockk<PolylineManager>(relaxed = true), null),
            Quad(null, null, null, mockk<GroundOverlayManager>(relaxed = true)),
        )

        for ((markerMgr, polygonMgr, polylineMgr, groundOverlayMgr) in managers) {
            var reattached = false
            createAndRenderScene(
                map = map,
                scene = scene,
                iconProvider = iconProvider,
                useAdvancedMarkers = false,
                markerManager = markerMgr,
                polygonManager = polygonMgr,
                polylineManager = polylineMgr,
                groundOverlayManager = groundOverlayMgr,
                reattach = { reattached = true },
            )
            assertTrue(reattached)
        }
    }

    @Test
    public fun testLoadLayerWithCallbacksInvokesOnLayerLoadedOnSuccess() {
        runTest {
            val expectedLayer = DataLayer(features = emptyList())
            var loadedCallbackValue: DataLayer? = null
            var capturedError: Throwable? = null

            val result = loadLayerWithCallbacks(
                defaultErrorMessage = "Failed",
                onLayerLoaded = { loadedCallbackValue = it },
                onError = { capturedError = it },
            ) {
                expectedLayer
            }

            assertSame(expectedLayer, result)
            assertSame(expectedLayer, loadedCallbackValue)
            assertNull(capturedError)
        }
    }

    @Test
    public fun testLoadLayerWithCallbacksCreatesDefaultErrorWhenLoaderReturnsNull() {
        runTest {
            var loadedCallbackInvoked = false
            var loadedCallbackValue: DataLayer? = DataLayer(features = emptyList())
            var capturedError: Throwable? = null

            val result = loadLayerWithCallbacks(
                defaultErrorMessage = "Asset missing: test.geojson",
                onLayerLoaded = {
                    loadedCallbackInvoked = true
                    loadedCallbackValue = it
                },
                onError = { capturedError = it },
            ) {
                null
            }

            assertNull(result)
            assertTrue(loadedCallbackInvoked)
            assertNull(loadedCallbackValue)
            assertTrue(capturedError is IllegalStateException)
            assertEquals("Asset missing: test.geojson", capturedError?.message)
        }
    }

    @Test
    public fun testLoadLayerWithCallbacksForwardsThrownExceptionToOnError() {
        runTest {
            val expectedException = IOException("Corrupt stream")
            var loadedCallbackValue: DataLayer? = DataLayer(features = emptyList())
            var capturedError: Throwable? = null

            val result = loadLayerWithCallbacks(
                defaultErrorMessage = "Default message",
                onLayerLoaded = { loadedCallbackValue = it },
                onError = { capturedError = it },
            ) {
                throw expectedException
            }

            assertNull(result)
            assertNull(loadedCallbackValue)
            assertSame(expectedException, capturedError)
        }
    }

    @Test
    public fun testDataLayerLoaderParsesGeoJsonAndKmlFromInputStream() {
        runTest {
            val context = mockk<Context>(relaxed = true)
            val dispatcher = UnconfinedTestDispatcher(testScheduler)
            val loader = DataLayerLoader(context, dispatcher)

            val validGeoJson = """
                {
                  "type": "FeatureCollection",
                  "features": [
                    {
                      "type": "Feature",
                      "id": "feature-1",
                      "geometry": {
                        "type": "Point",
                        "coordinates": [-122.084, 37.422]
                      },
                      "properties": {
                        "title": "Googleplex"
                      }
                    }
                  ]
                }
            """.trimIndent()

            val geoJsonLayer = loader.loadInputStream(
                inputStream = ByteArrayInputStream(validGeoJson.toByteArray()),
                fileName = "points.geojson",
            )
            assertNotNull(geoJsonLayer)
            assertEquals(1, geoJsonLayer!!.features.size)
            assertEquals("Googleplex", geoJsonLayer.features.first().properties["title"])

            val validKml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <kml xmlns="http://www.opengis.net/kml/2.2">
                  <Document>
                    <Placemark>
                      <name>Mountain View</name>
                      <Point>
                        <coordinates>-122.084,37.422,0</coordinates>
                      </Point>
                    </Placemark>
                  </Document>
                </kml>
            """.trimIndent()

            val kmlLayer = loader.loadInputStream(
                inputStream = ByteArrayInputStream(validKml.toByteArray()),
                fileName = "places.kml",
            )
            assertNotNull(kmlLayer)
            assertEquals(1, kmlLayer!!.features.size)
            assertEquals("Mountain View", kmlLayer.features.first().properties["name"])

            val invalidLayer = loader.loadInputStream(
                inputStream = ByteArrayInputStream("not valid data {{{".toByteArray()),
                fileName = "unknown.txt",
            )
            assertNull(invalidLayer)
        }
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
