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

package com.google.maps.android.compose.heatmaps

import com.google.maps.android.heatmaps.Gradient
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

public class HeatmapTileOverlayTest {

    @Test
    public fun testGradientsEqual() {
        val g1 = Gradient(
            intArrayOf(0xFF00FF00.toInt(), 0xFFFF0000.toInt()),
            floatArrayOf(0.2f, 1.0f),
            1000,
        )
        val g2 = Gradient(
            intArrayOf(0xFF00FF00.toInt(), 0xFFFF0000.toInt()),
            floatArrayOf(0.2f, 1.0f),
            1000,
        )
        val gDifferentColors = Gradient(
            intArrayOf(0xFF0000FF.toInt(), 0xFFFF0000.toInt()),
            floatArrayOf(0.2f, 1.0f),
            1000,
        )
        val gDifferentPoints = Gradient(
            intArrayOf(0xFF00FF00.toInt(), 0xFFFF0000.toInt()),
            floatArrayOf(0.5f, 1.0f),
            1000,
        )
        val gDifferentSize = Gradient(
            intArrayOf(0xFF00FF00.toInt(), 0xFFFF0000.toInt()),
            floatArrayOf(0.2f, 1.0f),
            500,
        )

        assertTrue(gradientsEqual(g1, g1))
        assertTrue(gradientsEqual(g1, g2))
        assertTrue(gradientsEqual(null, null))

        assertFalse(gradientsEqual(g1, null))
        assertFalse(gradientsEqual(null, g1))
        assertFalse(gradientsEqual(g1, gDifferentColors))
        assertFalse(gradientsEqual(g1, gDifferentPoints))
        assertFalse(gradientsEqual(g1, gDifferentSize))
    }
}
