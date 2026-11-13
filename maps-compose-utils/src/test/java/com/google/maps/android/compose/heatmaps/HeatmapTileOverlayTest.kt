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

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.TileOverlayState
import com.google.maps.android.heatmaps.Gradient
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.heatmaps.WeightedLatLng
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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

    @Test
    public fun testCreateHeatmapTileProviderBuildsValidProvider() {
        val points = listOf(
            WeightedLatLng(LatLng(37.422, -122.084), 1.5),
            WeightedLatLng(LatLng(37.423, -122.085), 2.5),
        )
        val provider = createHeatmapTileProvider(
            weightedData = points,
            radius = HeatmapTileProvider.DEFAULT_RADIUS,
            gradient = HeatmapTileProvider.DEFAULT_GRADIENT,
            opacity = HeatmapTileProvider.DEFAULT_OPACITY,
            maxIntensity = 5.0,
        )
        assertNotNull(provider)
    }

    @Test
    public fun testHeatmapParamsApplyUpdatesOnlyMutatesChangedPropertiesAndClearsCache() {
        val initialPoints = listOf(WeightedLatLng(LatLng(37.422, -122.084), 1.0))
        val updatedPoints = listOf(
            WeightedLatLng(LatLng(37.422, -122.084), 1.0),
            WeightedLatLng(LatLng(37.425, -122.088), 3.0),
        )
        val initialGradient = Gradient(
            intArrayOf(0xFF00FF00.toInt(), 0xFFFF0000.toInt()),
            floatArrayOf(0.2f, 1.0f),
            1000,
        )
        val equalGradientCopy = Gradient(
            intArrayOf(0xFF00FF00.toInt(), 0xFFFF0000.toInt()),
            floatArrayOf(0.2f, 1.0f),
            1000,
        )
        val newGradient = Gradient(
            intArrayOf(0xFF0000FF.toInt(), 0xFFFFFF00.toInt()),
            floatArrayOf(0.1f, 0.9f),
            1000,
        )

        val provider = mockk<HeatmapTileProvider>(relaxed = true)
        val state = mockk<TileOverlayState>(relaxed = true)
        every { state.clearTileCache() } just runs

        val params = HeatmapParams(
            radius = 20,
            opacity = 0.7,
            maxIntensity = 0.0,
            gradient = initialGradient,
            weightedData = initialPoints,
        )

        // 1. No changes (even with a new structurally equal Gradient instance) -> returns false and does not clear cache
        val unchanged = params.applyUpdates(
            provider = provider,
            state = state,
            radius = 20,
            opacity = 0.7,
            maxIntensity = 0.0,
            gradient = equalGradientCopy,
            weightedData = initialPoints,
        )
        assertFalse(unchanged)
        verify(exactly = 0) { state.clearTileCache() }

        // 2. Mutate all 5 properties -> returns true, updates provider & params, and clears tile cache
        val changed = params.applyUpdates(
            provider = provider,
            state = state,
            radius = 35,
            opacity = 0.5,
            maxIntensity = 10.0,
            gradient = newGradient,
            weightedData = updatedPoints,
        )
        assertTrue(changed)
        assertEquals(35, params.radius)
        assertEquals(0.5, params.opacity, 0.0)
        assertEquals(10.0, params.maxIntensity, 0.0)
        assertSame(newGradient, params.gradient)
        assertSame(updatedPoints, params.weightedData)

        verify(exactly = 1) { provider.setRadius(35) }
        verify(exactly = 1) { provider.setOpacity(0.5) }
        verify(exactly = 1) { provider.setMaxIntensity(10.0) }
        verify(exactly = 1) { provider.setGradient(newGradient) }
        verify(exactly = 1) { provider.updateData(updatedPoints) }
        verify(exactly = 1) { state.clearTileCache() }
    }
}
