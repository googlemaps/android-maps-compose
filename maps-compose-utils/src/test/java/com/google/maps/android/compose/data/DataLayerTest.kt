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

import android.graphics.Bitmap
import com.google.maps.android.compose.data.geojson.parseGeoJsonLayer
import com.google.maps.android.compose.data.kml.parseKmlOrKmzStream
import com.google.maps.android.data.parser.kml.ImageDecoder
import com.google.maps.android.data.renderer.mapview.MapViewRenderer
import com.google.maps.android.data.renderer.model.DataLayer
import com.google.maps.android.data.renderer.model.DataScene
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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
    public fun testParseGeoJsonLayerParsesValidGeoJsonAndReturnsNullOnMalformedInput() {
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

        val layer = parseGeoJsonLayer(validGeoJson)
        assertNotNull(layer)
        assertEquals(1, layer!!.features.size)
        assertEquals("Googleplex", layer.features.first().properties["title"])

        val invalidLayer = parseGeoJsonLayer("not valid json {{{")
        assertNull(invalidLayer)
    }

    @Test
    public fun testParseKmlOrKmzStreamParsesKmlAndEnforcesKmzLimits() {
        val kmlContent = """
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

        val kmlLayer = parseKmlOrKmzStream(
            inputStream = ByteArrayInputStream(kmlContent.toByteArray()),
            imageDecoder = object : ImageDecoder {
                override fun decode(bytes: ByteArray): Bitmap? = null
            },
        )
        assertEquals(1, kmlLayer.features.size)
        assertEquals("Mountain View", kmlLayer.features.first().properties["name"])

        // Create a KMZ archive in-memory with 2 entries
        val kmzBytes = ByteArrayOutputStream().apply {
            ZipOutputStream(this).use { zos ->
                zos.putNextEntry(ZipEntry("doc.kml"))
                zos.write(kmlContent.toByteArray())
                zos.closeEntry()
                zos.putNextEntry(ZipEntry("images/icon.png"))
                zos.write(byteArrayOf(1, 2, 3, 4))
                zos.closeEntry()
            }
        }.toByteArray()

        val fakeBitmap = mockk<Bitmap>()
        val fakeDecoder = object : ImageDecoder {
            override fun decode(bytes: ByteArray): Bitmap = fakeBitmap
        }
        val kmzLayer = parseKmlOrKmzStream(
            inputStream = ByteArrayInputStream(kmzBytes),
            imageDecoder = fakeDecoder,
        )
        assertEquals(1, kmzLayer.features.size)
        val images = kmzLayer.properties["images"] as? Map<*, *>
        assertNotNull(images)
        assertEquals(fakeBitmap, images!!["images/icon.png"])

        // Enforcing maxKmzEntryCount = 1 should throw IOException
        assertThrows(IOException::class.java) {
            parseKmlOrKmzStream(
                inputStream = ByteArrayInputStream(kmzBytes),
                maxKmzEntryCount = 1,
                imageDecoder = fakeDecoder,
            )
        }
    }
}
