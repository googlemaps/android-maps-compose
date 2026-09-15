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

package com.google.maps.android.compose.wms

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.net.URL

// [START maps_compose_utils_wms_url_tile_provider_test]
public class WmsUrlTileProviderTest {

    private val worldSize: Double = 6378137.0 * kotlin.math.PI

    @Test
    public fun testGetBoundingBoxZoom0(): Unit {
        val provider = WmsUrlTileProvider { _, _, _, _, _ -> "" }
        val bbox = provider.getBoundingBox(0, 0, 0)

        // Zoom 0, Tile 0,0 should cover the entire world
        val expected = doubleArrayOf(-worldSize, -worldSize, worldSize, worldSize)
        assertArrayEquals(expected, bbox, 0.001)
    }

    @Test
    public fun testGetBoundingBoxZoom1(): Unit {
        val provider = WmsUrlTileProvider { _, _, _, _, _ -> "" }

        // Zoom 1, Tile 0,0 (Top Left)
        val bbox00 = provider.getBoundingBox(0, 0, 1)
        val expected00 = doubleArrayOf(-worldSize, 0.0, 0.0, worldSize)
        assertArrayEquals(expected00, bbox00, 0.001)

        // Zoom 1, Tile 1,1 (Bottom Right)
        val bbox11 = provider.getBoundingBox(1, 1, 1)
        val expected11 = doubleArrayOf(0.0, -worldSize, worldSize, 0.0)
        assertArrayEquals(expected11, bbox11, 0.001)
    }

    @Test
    public fun testGetBoundingBoxSpecificTile(): Unit {
        val provider = WmsUrlTileProvider { _, _, _, _, _ -> "" }

        // Zoom 2, Tile 1,1
        // Num tiles = 4x4. Tile size = 2 * worldSize / 4 = worldSize / 2
        // xMin = -worldSize + 1 * (worldSize/2) = -worldSize/2
        // xMax = -worldSize + 2 * (worldSize/2) = 0
        // yMax = worldSize - 1 * (worldSize/2) = worldSize/2
        // yMin = worldSize - 2 * (worldSize/2) = 0
        val bbox = provider.getBoundingBox(1, 1, 2)
        val expected = doubleArrayOf(-worldSize / 2, 0.0, 0.0, worldSize / 2)
        assertArrayEquals(expected, bbox, 0.001)
    }

    @Test
    public fun testGetTileUrl_validUrl_returnsParsedUrl(): Unit {
        val provider = WmsUrlTileProvider { xMin, yMin, xMax, yMax, zoom ->
            "https://example.com/wms?bbox=$xMin,$yMin,$xMax,$yMax&zoom=$zoom"
        }
        val tileUrl = provider.getTileUrl(0, 0, 1)
        assertThat(tileUrl).isNotNull()
        assertThat(tileUrl).isEqualTo(
            URL("https://example.com/wms?bbox=-${worldSize},0.0,0.0,${worldSize}&zoom=1")
        )
    }

    @Test
    public fun testGetTileUrl_malformedUrl_returnsNull(): Unit {
        val provider = WmsUrlTileProvider { _, _, _, _, _ ->
            "not a valid url ://"
        }
        val tileUrl = provider.getTileUrl(0, 0, 1)
        assertThat(tileUrl).isNull()
    }
}
// [END maps_compose_utils_wms_url_tile_provider_test]

