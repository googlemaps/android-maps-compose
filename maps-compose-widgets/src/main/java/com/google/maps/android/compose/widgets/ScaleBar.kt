// Copyright 2022 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.maps.android.compose.widgets

import android.graphics.Point
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.ImperialUnitsConverter
import com.google.maps.android.MetricUnitsConverter
import com.google.maps.android.NumberFormatSymbols
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.meters
import com.google.maps.android.miles
import com.google.maps.android.sphericalDistance
import com.google.maps.android.toDistanceString
import kotlinx.coroutines.delay

/**
 * Computes the ground distance (in meters) represented by the horizontal bar of the [ScaleBar].
 *
 * Google Maps uses a Web Mercator projection (`EPSG:3857`), which is conformal: at any latitude
 * `φ`, the map scale stretches isotropically (in both East–West and North–South directions) by a
 * factor of `sec(φ) = 1 / cos(φ)`. Because of this latitude-dependent distortion—as well as
 * perspective foreshortening when the camera is tilted—the ground distance per screen pixel can
 * vary dramatically between the top edge of the screen, the center of the viewport, and the bottom
 * edge of the screen (especially at low zoom levels or high latitudes).
 *
 * When a non-null [target] (typically `cameraPositionState.position.target`) is supplied, this
 * function projects that geographic target to its screen coordinate `centerPoint` and measures a
 * horizontal pixel span of [width] centered on `(centerPoint.x, centerPoint.y)`:
 * - This measures the map scale at the exact focal point the user is looking at.
 * - Because the span is sampled horizontally in screen space across `centerPoint.y` and converted
 *   back to geographic coordinates via [Projection.fromScreenLocation], it automatically accounts
 *   for map bearing rotation, camera tilt, and device orientation changes.
 *
 * When [target] is `null` (used when `measureAtCenter = false`), this function falls back to the
 * legacy behavior of sampling horizontally along the top-left edge of the viewport from
 * `Point(0, 0)` to `Point(widthInPixels, 0)`.
 *
 * Finally, because the drawn horizontal scale line spans `8 / 9` of the canvas width (from
 * `size.width / 9` to `size.width`), the full canvas ground distance is scaled by `8 / 9`.
 */
internal fun calculateDistance(projection: Projection, width: Dp, density: Density, target: LatLng? = null): Int {
    val widthInPixels = with(density) {
        width.toPx().toInt()
    }

    val startPoint: Point
    val endPoint: Point
    if (target != null) {
        val centerPoint = projection.toScreenLocation(target)
        val halfWidthInPixels = widthInPixels / 2
        startPoint = Point(centerPoint.x - halfWidthInPixels, centerPoint.y)
        endPoint = Point(startPoint.x + widthInPixels, centerPoint.y)
    } else {
        startPoint = Point(0, 0)
        endPoint = Point(widthInPixels, 0)
    }

    val startLatLng = projection.fromScreenLocation(startPoint)
    val endLatLng = projection.fromScreenLocation(endPoint)

    val canvasWidthMeters = startLatLng.sphericalDistance(endLatLng)

    return (canvasWidthMeters * 8 / 9).toInt()
}

public val DarkGray: Color = Color(0xFF3a3c3b)
private val defaultWidth: Dp = 65.dp
private val defaultHeight: Dp = 50.dp

@Composable
@Deprecated("Retained for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun ScaleBar(
    modifier: Modifier = Modifier,
    width: Dp = defaultWidth,
    height: Dp = defaultHeight,
    cameraPositionState: CameraPositionState,
    textColor: Color = DarkGray,
    lineColor: Color = DarkGray,
    shadowColor: Color = Color.White,
) {
    ScaleBar(
        modifier = modifier,
        width = width,
        height = height,
        cameraPositionState = cameraPositionState,
        textColor = textColor,
        lineColor = lineColor,
        shadowColor = shadowColor,
        measureAtCenter = true,
    )
}

/**
 * A scale bar composable that shows the current scale of the map in feet and meters when zoomed in
 * to the map, changing to miles and kilometers, respectively, when zooming out.
 *
 * @param modifier Modifier to be applied to the composable.
 * @param width The width of the composable.
 * @param height The height of the composable.
 * @param cameraPositionState The state of the camera position, used to calculate the scale.
 * @param textColor The color of the text on the scale bar.
 * @param lineColor The color of the lines on the scale bar.
 * @param shadowColor The color of the shadow behind the text and lines.
 * @param measureAtCenter When `true` (the default), the scale distance is measured across the
 *   camera target at the center of the map viewport. When `false`, the legacy behavior is used,
 *   measuring the scale along the top edge of the map from `Point(0, 0)`. Because the Web Mercator
 *   projection stretches map scale by `1 / cos(latitude)` toward the poles, measuring at the
 *   center of the viewport more accurately reflects the scale where the camera is focused.
 */
@Composable
public fun ScaleBar(
    modifier: Modifier = Modifier,
    width: Dp = defaultWidth,
    height: Dp = defaultHeight,
    cameraPositionState: CameraPositionState,
    textColor: Color = DarkGray,
    lineColor: Color = DarkGray,
    shadowColor: Color = Color.White,
    measureAtCenter: Boolean = true,
) {
    // Observe LocalConfiguration so that device rotation or window resizing (which changes the
    // viewport dimensions and Projection screen coordinates without necessarily changing
    // CameraPosition) triggers a recalculation.
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val horizontalLineWidthMeters by remember(
        cameraPositionState,
        width,
        density,
        configuration,
        measureAtCenter,
    ) {
        derivedStateOf {
            // Read position so the scale updates whenever the camera pans, zooms, rotates, or tilts.
            val position = cameraPositionState.position
            val target = if (measureAtCenter) position.target else null
            cameraPositionState.projection?.let {
                calculateDistance(it, width, density, target)
            } ?: 0
        }
    }

    Box(
        modifier = modifier.size(width = width, height = height),
    ) {
        // The Canvas composable is used for custom drawing. Here, we are drawing the
        // lines of the scale bar.
        Canvas(
            modifier = Modifier.fillMaxSize(),
            onDraw = {
                val oneNinthWidth = size.width / 9
                val midHeight = size.height / 2
                val oneThirdHeight = size.height / 3
                val twoThirdsHeight = size.height * 2 / 3
                val strokeWidth = 4f
                val shadowStrokeWidth = strokeWidth + 3

                // The shadows are drawn first, slightly offset from the main lines, to create
                // a "drop shadow" effect. This makes the scale bar more readable on different
                // map backgrounds.

                // Middle horizontal line shadow
                drawLine(
                    color = shadowColor,
                    start = Offset(oneNinthWidth, midHeight),
                    end = Offset(size.width, midHeight),
                    strokeWidth = shadowStrokeWidth,
                    cap = StrokeCap.Round,
                )
                // Top vertical line shadow
                drawLine(
                    color = shadowColor,
                    start = Offset(oneNinthWidth, oneThirdHeight),
                    end = Offset(oneNinthWidth, midHeight),
                    strokeWidth = shadowStrokeWidth,
                    cap = StrokeCap.Round,
                )
                // Bottom vertical line shadow
                drawLine(
                    color = shadowColor,
                    start = Offset(oneNinthWidth, midHeight),
                    end = Offset(oneNinthWidth, twoThirdsHeight),
                    strokeWidth = shadowStrokeWidth,
                    cap = StrokeCap.Round,
                )

                // These are the main lines of the scale bar.

                // Middle horizontal line
                drawLine(
                    color = lineColor,
                    start = Offset(oneNinthWidth, midHeight),
                    end = Offset(size.width, midHeight),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
                // Top vertical line
                drawLine(
                    color = lineColor,
                    start = Offset(oneNinthWidth, oneThirdHeight),
                    end = Offset(oneNinthWidth, midHeight),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
                // Bottom vertical line
                drawLine(
                    color = lineColor,
                    start = Offset(oneNinthWidth, midHeight),
                    end = Offset(oneNinthWidth, twoThirdsHeight),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            },
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceAround,
        ) {
            // We display the calculated distances in two Text composables, one for imperial
            // and one for metric units.
            ScaleText(
                modifier = Modifier.align(End),
                textColor = textColor,
                shadowColor = shadowColor,
                text = formatImperialDistance(horizontalLineWidthMeters),
            )
            ScaleText(
                modifier = Modifier.align(End),
                textColor = textColor,
                shadowColor = shadowColor,
                text = formatMetricDistance(horizontalLineWidthMeters),
            )
        }
    }
}

// Use a 1-mile threshold so the imperial scale bar transitions from feet to miles at 1 mile
// (5,280 ft), matching the metric transition from meters to kilometers at 1 km (1,000 m), with
// compact US/invariant number formatting (`NumberFormatSymbols.COMPACT`) so labels fit inside the
// compact scale bar canvas.
private val scaleBarImperialConverter = ImperialUnitsConverter(milesThreshold = 1.miles)

internal fun formatMetricDistance(meters: Int): String =
    meters.meters.toDistanceString(MetricUnitsConverter, NumberFormatSymbols.COMPACT)

internal fun formatImperialDistance(meters: Int): String =
    meters.meters.toDistanceString(scaleBarImperialConverter, NumberFormatSymbols.COMPACT)

@Composable
@Deprecated("Retained for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun DisappearingScaleBar(
    modifier: Modifier = Modifier,
    width: Dp = defaultWidth,
    height: Dp = defaultHeight,
    cameraPositionState: CameraPositionState,
    textColor: Color = DarkGray,
    lineColor: Color = DarkGray,
    shadowColor: Color = Color.White,
    visibilityDurationMillis: Int = 3_000,
    enterTransition: EnterTransition = fadeIn(),
    exitTransition: ExitTransition = fadeOut(),
) {
    DisappearingScaleBar(
        modifier = modifier,
        width = width,
        height = height,
        cameraPositionState = cameraPositionState,
        textColor = textColor,
        lineColor = lineColor,
        shadowColor = shadowColor,
        visibilityDurationMillis = visibilityDurationMillis,
        enterTransition = enterTransition,
        exitTransition = exitTransition,
        measureAtCenter = true,
    )
}

/**
 * An animated scale bar that appears when the zoom level of the map changes, and then disappears
 * after [visibilityDurationMillis]. This composable wraps [ScaleBar] with visibility animations.
 *
 * @param modifier Modifier to be applied to the composable.
 * @param width The width of the composable.
 * @param height The height of the composable.
 * @param cameraPositionState The state of the camera position, used to calculate the scale.
 * @param textColor The color of the text on the scale bar.
 * @param lineColor The color of the lines on the scale bar.
 * @param shadowColor The color of the shadow behind the text and lines.
 * @param visibilityDurationMillis The duration in milliseconds that the scale bar will be visible.
 * @param enterTransition The animation to use when the scale bar appears.
 * @param exitTransition The animation to use when the scale bar disappears.
 * @param measureAtCenter When `true` (the default), the scale distance is measured across the
 *   camera target at the center of the map viewport. When `false`, the legacy behavior is used,
 *   measuring the scale along the top edge of the map from `Point(0, 0)`.
 */
@Composable
public fun DisappearingScaleBar(
    modifier: Modifier = Modifier,
    width: Dp = defaultWidth,
    height: Dp = defaultHeight,
    cameraPositionState: CameraPositionState,
    textColor: Color = DarkGray,
    lineColor: Color = DarkGray,
    shadowColor: Color = Color.White,
    visibilityDurationMillis: Int = 3_000,
    enterTransition: EnterTransition = fadeIn(),
    exitTransition: ExitTransition = fadeOut(),
    measureAtCenter: Boolean = true,
) {
    val visible = remember {
        MutableTransitionState(true)
    }

    // This effect is re-launched every time the camera position changes.
    //
    // The effect itself makes the scale bar visible, waits for the specified duration,
    // and then makes it invisible again. This creates the "disappearing" effect.
    LaunchedEffect(key1 = cameraPositionState.position) {
        visible.targetState = true
        delay(visibilityDurationMillis.toLong())
        visible.targetState = false
    }

    // `AnimatedVisibility` is a composable that animates the appearance and disappearance
    // of its content. We are using it here to wrap the `ScaleBar` and provide the
    // fade-in and fade-out animations.
    AnimatedVisibility(
        visibleState = visible,
        modifier = modifier,
        enter = enterTransition,
        exit = exitTransition,
    ) {
        ScaleBar(
            width = width,
            height = height,
            cameraPositionState = cameraPositionState,
            textColor = textColor,
            lineColor = lineColor,
            shadowColor = shadowColor,
            measureAtCenter = measureAtCenter,
        )
    }
}

@Composable
private fun ScaleText(
    modifier: Modifier = Modifier,
    text: String,
    textColor: Color = DarkGray,
    shadowColor: Color = Color.White,
) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = textColor,
        textAlign = TextAlign.End,
        lineHeight = 1.em,
        modifier = modifier,
        style = MaterialTheme.typography.h4.copy(
            shadow = Shadow(
                color = shadowColor,
                offset = Offset(2f, 2f),
                blurRadius = 1f,
            ),
        ),
    )
}
