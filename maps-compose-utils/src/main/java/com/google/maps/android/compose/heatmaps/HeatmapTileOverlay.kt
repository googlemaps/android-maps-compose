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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.TileOverlay
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MapsComposeExperimentalApi
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
@MapsComposeExperimentalApi
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
@MapsComposeExperimentalApi
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

    val provider = remember {
        HeatmapTileProvider.Builder()
            .weightedData(weightedData)
            .radius(radius)
            .gradient(gradient)
            .opacity(opacity)
            .maxIntensity(maxIntensity)
            .build()
    }

    val currentParams = remember {
        HeatmapParams(
            radius = radius,
            opacity = opacity,
            maxIntensity = maxIntensity,
            gradient = gradient,
            weightedData = weightedData,
        )
    }

    SideEffect {
        var changed = false
        if (currentParams.radius != radius) {
            provider.setRadius(radius)
            currentParams.radius = radius
            changed = true
        }
        if (currentParams.opacity != opacity) {
            provider.setOpacity(opacity)
            currentParams.opacity = opacity
            changed = true
        }
        if (currentParams.maxIntensity != maxIntensity) {
            provider.setMaxIntensity(maxIntensity)
            currentParams.maxIntensity = maxIntensity
            changed = true
        }
        if (!gradientsEqual(currentParams.gradient, gradient)) {
            provider.setGradient(gradient)
            currentParams.gradient = gradient
            changed = true
        }
        if (currentParams.weightedData != weightedData) {
            provider.updateData(weightedData)
            currentParams.weightedData = weightedData
            changed = true
        }
        if (changed) {
            runCatching { state.clearTileCache() }
        }
    }

    TileOverlay(
        tileProvider = provider,
        state = state,
        fadeIn = fadeIn,
        transparency = transparency,
        visible = visible,
        zIndex = zIndex,
        onClick = onClick,
    )
}

private class HeatmapParams(
    var radius: Int,
    var opacity: Double,
    var maxIntensity: Double,
    var gradient: Gradient,
    var weightedData: Collection<WeightedLatLng>,
)

internal fun gradientsEqual(g1: Gradient?, g2: Gradient?): Boolean {
    if (g1 === g2) return true
    if (g1 == null || g2 == null) return false
    return g1.colorMapSize == g2.colorMapSize &&
        g1.colors.contentEquals(g2.colors) &&
        g1.startPoints.contentEquals(g2.startPoints)
}
