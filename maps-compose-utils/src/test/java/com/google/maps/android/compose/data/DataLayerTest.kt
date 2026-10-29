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
import com.google.maps.android.data.DataLayerLoader
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream

@OptIn(ExperimentalCoroutinesApi::class)
public class DataLayerTest {

    @Test
    public fun testRenderSceneWithCachedImagesCachesBitmapsBeforeRender() {
        val renderer = mockk<MapViewRenderer>(relaxed = true)
        val bitmap = mockk<Bitmap>()
        every { renderer.cacheImageData(any(), any()) } just runs
        every { renderer.render(any()) } just runs

        val layer = DataLayer(
            features = emptyList(),
            properties = mapOf("images" to mapOf("icons/pin.png" to bitmap)),
        )
        val scene = DataScene(listOf(layer))

        renderSceneWithCachedImages(renderer, scene)

        verify { renderer.cacheImageData("icons/pin.png", bitmap) }
        verify { renderer.render(scene) }
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
}
