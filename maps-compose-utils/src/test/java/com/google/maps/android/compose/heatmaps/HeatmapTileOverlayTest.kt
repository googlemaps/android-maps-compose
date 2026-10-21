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
import com.google.maps.android.heatmaps.Gradient
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.heatmaps.WeightedLatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

public class HeatmapTileOverlayTest {

    @Test
    public fun testUpdateHeatmapProviderHolderCreatesAndUpdatesProviderInPlace() {
        val initialPoints = listOf(
            WeightedLatLng(LatLng(37.7749, -122.4194), 1.0),
            WeightedLatLng(LatLng(37.7849, -122.4094), 2.0),
        )

        val holder = updateHeatmapProviderHolder(
            existing = null,
            weightedData = initialPoints,
            radius = HeatmapTileProvider.DEFAULT_RADIUS,
            gradient = HeatmapTileProvider.DEFAULT_GRADIENT,
            opacity = HeatmapTileProvider.DEFAULT_OPACITY,
            maxIntensity = 0.0,
        )

        assertNotNull(holder.provider)
        assertEquals(0, holder.version)

        // Calling with identical values should not increment version
        val unchangedHolder = updateHeatmapProviderHolder(
            existing = holder,
            weightedData = initialPoints,
            radius = HeatmapTileProvider.DEFAULT_RADIUS,
            gradient = HeatmapTileProvider.DEFAULT_GRADIENT,
            opacity = HeatmapTileProvider.DEFAULT_OPACITY,
            maxIntensity = 0.0,
        )
        assertSame(holder, unchangedHolder)
        assertEquals(0, unchangedHolder.version)

        // Updating radius, opacity, maxIntensity, gradient, and data should reuse provider and bump version
        val updatedPoints = listOf(
            WeightedLatLng(LatLng(34.0522, -118.2437), 3.0),
        )
        val customGradient = Gradient(
            intArrayOf(0xFF00FF00.toInt(), 0xFFFF0000.toInt()),
            floatArrayOf(0.1f, 0.9f),
        )
        val updatedHolder = updateHeatmapProviderHolder(
            existing = holder,
            weightedData = updatedPoints,
            radius = 30,
            gradient = customGradient,
            opacity = 0.5,
            maxIntensity = 10.0,
        )
        assertSame(holder, updatedHolder)
        assertEquals(1, updatedHolder.version)
        assertEquals(30, updatedHolder.radius)
        assertEquals(0.5, updatedHolder.opacity, 0.0001)
        assertEquals(10.0, updatedHolder.maxIntensity, 0.0001)
        assertSame(customGradient, updatedHolder.gradient)
        assertSame(updatedPoints, updatedHolder.weightedData)
    }
}
