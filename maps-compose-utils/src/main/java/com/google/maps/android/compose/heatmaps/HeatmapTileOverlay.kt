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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.TileOverlay
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.TileOverlay
import com.google.maps.android.compose.TileOverlayState
import com.google.maps.android.compose.rememberTileOverlayState
import com.google.maps.android.heatmaps.Gradient
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.heatmaps.WeightedLatLng

/**
 * A Composable that displays a heatmap tile overlay on the map from a collection of unweighted
 * [LatLng] coordinates.
 *
 * If [data] is empty, no tile overlay is rendered until points are provided.
 *
 * @param data the collection of [LatLng] points to display in the heatmap.
 * @param state the [TileOverlayState] used to control the tile overlay.
 * @param radius the radius of the Gaussian blur in pixels, between [HeatmapTileProvider.MIN_RADIUS]
 * and [HeatmapTileProvider.MAX_RADIUS].
 * @param gradient the color [Gradient] of the heatmap.
 * @param opacity the opacity of the heatmap layer in the range `[0.0, 1.0]`.
 * @param maxIntensity the custom maximum intensity value for the heatmap (`0.0` uses the default
 * auto-computed maximum).
 * @param fadeIn boolean indicating whether the tiles should fade in.
 * @param transparency the transparency of the tile overlay.
 * @param visible the visibility of the tile overlay.
 * @param zIndex the z-index of the tile overlay.
 * @param onClick a lambda invoked when the tile overlay is clicked.
 */
@Composable
@GoogleMapComposable
public fun HeatmapTileOverlay(
    data: Collection<LatLng>,
    state: TileOverlayState = rememberTileOverlayState(),
    radius: Int = HeatmapTileProvider.DEFAULT_RADIUS,
    gradient: Gradient = HeatmapTileProvider.DEFAULT_GRADIENT,
    opacity: Double = HeatmapTileProvider.DEFAULT_OPACITY,
    maxIntensity: Double = 0.0,
    fadeIn: Boolean = true,
    transparency: Float = 0f,
    visible: Boolean = true,
    zIndex: Float = 0f,
    onClick: (TileOverlay) -> Unit = {},
) {
    val weightedData = remember(data) {
        data.map { WeightedLatLng(it) }
    }
    HeatmapTileOverlay(
        weightedData = weightedData,
        state = state,
        radius = radius,
        gradient = gradient,
        opacity = opacity,
        maxIntensity = maxIntensity,
        fadeIn = fadeIn,
        transparency = transparency,
        visible = visible,
        zIndex = zIndex,
        onClick = onClick,
    )
}

/**
 * A Composable that displays a heatmap tile overlay on the map from a collection of
 * [WeightedLatLng] points.
 *
 * If [weightedData] is empty, no tile overlay is rendered until points are provided.
 *
 * @param weightedData the collection of [WeightedLatLng] points to display in the heatmap.
 * @param state the [TileOverlayState] used to control the tile overlay.
 * @param radius the radius of the Gaussian blur in pixels, between [HeatmapTileProvider.MIN_RADIUS]
 * and [HeatmapTileProvider.MAX_RADIUS].
 * @param gradient the color [Gradient] of the heatmap.
 * @param opacity the opacity of the heatmap layer in the range `[0.0, 1.0]`.
 * @param maxIntensity the custom maximum intensity value for the heatmap (`0.0` uses the default
 * auto-computed maximum).
 * @param fadeIn boolean indicating whether the tiles should fade in.
 * @param transparency the transparency of the tile overlay.
 * @param visible the visibility of the tile overlay.
 * @param zIndex the z-index of the tile overlay.
 * @param onClick a lambda invoked when the tile overlay is clicked.
 */
@Composable
@GoogleMapComposable
@JvmName("WeightedHeatmapTileOverlay")
public fun HeatmapTileOverlay(
    weightedData: Collection<WeightedLatLng>,
    state: TileOverlayState = rememberTileOverlayState(),
    radius: Int = HeatmapTileProvider.DEFAULT_RADIUS,
    gradient: Gradient = HeatmapTileProvider.DEFAULT_GRADIENT,
    opacity: Double = HeatmapTileProvider.DEFAULT_OPACITY,
    maxIntensity: Double = 0.0,
    fadeIn: Boolean = true,
    transparency: Float = 0f,
    visible: Boolean = true,
    zIndex: Float = 0f,
    onClick: (TileOverlay) -> Unit = {},
) {
    if (weightedData.isEmpty()) return

    var holder by remember { mutableStateOf<HeatmapProviderHolder?>(null) }
    val currentHolder = updateHeatmapProviderHolder(
        existing = holder,
        weightedData = weightedData,
        radius = radius,
        gradient = gradient,
        opacity = opacity,
        maxIntensity = maxIntensity,
    )
    if (holder !== currentHolder) {
        holder = currentHolder
    }

    TileOverlay(
        tileProvider = currentHolder.provider,
        state = state,
        fadeIn = fadeIn,
        transparency = transparency,
        visible = visible,
        zIndex = zIndex,
        onClick = onClick,
    )

    LaunchedEffect(currentHolder.version) {
        if (currentHolder.version > 0) {
            runCatching { state.clearTileCache() }
        }
    }
}

internal class HeatmapProviderHolder(
    val provider: HeatmapTileProvider,
    var weightedData: Collection<WeightedLatLng>,
    var radius: Int,
    var gradient: Gradient,
    var opacity: Double,
    var maxIntensity: Double,
    var version: Int = 0,
)

internal fun updateHeatmapProviderHolder(
    existing: HeatmapProviderHolder?,
    weightedData: Collection<WeightedLatLng>,
    radius: Int,
    gradient: Gradient,
    opacity: Double,
    maxIntensity: Double,
): HeatmapProviderHolder {
    if (existing == null) {
        val provider = HeatmapTileProvider.Builder()
            .weightedData(weightedData)
            .radius(radius)
            .gradient(gradient)
            .opacity(opacity)
            .maxIntensity(maxIntensity)
            .build()
        return HeatmapProviderHolder(
            provider = provider,
            weightedData = weightedData,
            radius = radius,
            gradient = gradient,
            opacity = opacity,
            maxIntensity = maxIntensity,
            version = 0,
        )
    }

    var changed = false
    if (existing.weightedData != weightedData) {
        existing.provider.updateData(weightedData)
        existing.weightedData = weightedData
        changed = true
    }
    if (existing.radius != radius) {
        existing.provider.setRadius(radius)
        existing.radius = radius
        changed = true
    }
    if (existing.gradient != gradient) {
        existing.provider.setGradient(gradient)
        existing.gradient = gradient
        changed = true
    }
    if (existing.opacity != opacity) {
        existing.provider.setOpacity(opacity)
        existing.opacity = opacity
        changed = true
    }
    if (existing.maxIntensity != maxIntensity) {
        existing.provider.setMaxIntensity(maxIntensity)
        existing.maxIntensity = maxIntensity
        changed = true
    }
    if (changed) {
        existing.version++
    }
    return existing
}
