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

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

public class WmsUrlTileProviderTest {

    private val worldSize: Double = 20037508.34789244

    @Test
    public fun testGetBoundingBoxZoom0() {
        val provider = WmsUrlTileProvider { _, _, _, _, _ -> "" }
        val bbox = provider.getBoundingBox(0, 0, 0)

        // Zoom 0, Tile 0,0 should cover the entire world
        val expected = doubleArrayOf(-worldSize, -worldSize, worldSize, worldSize)
        assertArrayEquals(expected, bbox, 0.001)
    }

    @Test
    public fun testGetBoundingBoxZoom1() {
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
    public fun testGetBoundingBoxSpecificTile() {
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
    public fun testGetTileUrlBeyondBounds() {
        val provider = WmsUrlTileProvider(datasetXMinBound = 1.0) { _, _, _, _, _ -> "https://example.com" }
        val halfOfRes = {zoom : Int -> 1 shl (zoom - 1)}
        for (z in 1..3) {
            for (x in 0..<halfOfRes(z)) { //since xMinBound is the line 1.0 the boxes made to reach 0.0 will not intersect
                for (y in 0..2 * halfOfRes(z)) {
                    val tileUrl = provider.getTileUrl(x, y, z)
                    assertNull(tileUrl)
                }
            }
        }
    }

    @Test
    public fun testGetTileUrlWithinBounds() {
        val provider = WmsUrlTileProvider(datasetXMaxBound = -1.0) { _, _, _, _, _ -> "https://example.com" }
        val halfOfRes = {zoom : Int -> 1 shl (zoom - 1)}
        for (z in 1..3) {
            for (x in 0..<halfOfRes(z)) { //since xMaxBound is the line 1.0 the boxes made to reach 0.0 will be contained
                for (y in 0..2 * halfOfRes(z)) {
                    val tileUrl = provider.getTileUrl(x, y, z)
                    assertNotNull(tileUrl)
                }
            }
        }
    }

}
