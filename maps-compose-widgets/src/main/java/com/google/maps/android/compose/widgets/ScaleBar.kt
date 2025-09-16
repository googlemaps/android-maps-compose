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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.ktx.utils.sphericalDistance
import kotlinx.coroutines.delay

public val DarkGray: Color = Color(0xFF3a3c3b)
private val defaultWidth: Dp = 65.dp
private val defaultHeight: Dp = 50.dp

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
) {
    // This is the core logic for calculating the scale of the map.
    //
    // `remember` with a key (`cameraPositionState.position.zoom`) is used for performance.
    // It ensures that the calculation inside is only re-executed when the zoom level changes.
    // This is important because we don't need to recalculate the scale every time the map pans,
    // only when the zoom level changes.
    //
    // `derivedStateOf` is a Compose state function that creates a new state object that is
    // derived from other state objects. The calculation inside `derivedStateOf` is only
    // re-executed when one of the state objects it reads from changes. In this case, it's
    // `cameraPositionState.projection`. This is another performance optimization that
    // prevents unnecessary recalculations.
    val horizontalLineWidthMeters by remember(cameraPositionState.position.zoom) {
        derivedStateOf {
            // The projection is used to convert between screen coordinates (pixels) and
            // geographical coordinates (LatLng). It can be null if the map is not ready yet.
            val projection = cameraPositionState.projection ?: return@derivedStateOf 0

            // We get the geographical coordinates of two points on the screen: the top-left
            // corner (0, 0) and a point to the right of it, at the width of the scale bar.
            val upperLeftLatLng = projection.fromScreenLocation(Point(0, 0))
            val upperRightLatLng =
                projection.fromScreenLocation(Point(0, width.value.toInt()))

            // We then calculate the spherical distance between these two points in meters.
            // This gives us the distance that the scale bar represents on the map.
            val canvasWidthMeters = upperLeftLatLng.sphericalDistance(upperRightLatLng)

            // We take 8/9th of the canvas width to provide some padding on the right side
            // of the scale bar.
            (canvasWidthMeters * 8 / 9).toInt()
        }
    }

    Box(
        modifier = modifier.size(width = width, height = height)
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
                    cap = StrokeCap.Round
                )
                // Top vertical line shadow
                drawLine(
                    color = shadowColor,
                    start = Offset(oneNinthWidth, oneThirdHeight),
                    end = Offset(oneNinthWidth, midHeight),
                    strokeWidth = shadowStrokeWidth,
                    cap = StrokeCap.Round
                )
                // Bottom vertical line shadow
                drawLine(
                    color = shadowColor,
                    start = Offset(oneNinthWidth, midHeight),
                    end = Offset(oneNinthWidth, twoThirdsHeight),
                    strokeWidth = shadowStrokeWidth,
                    cap = StrokeCap.Round
                )

                // These are the main lines of the scale bar.

                // Middle horizontal line
                drawLine(
                    color = lineColor,
                    start = Offset(oneNinthWidth, midHeight),
                    end = Offset(size.width, midHeight),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                // Top vertical line
                drawLine(
                    color = lineColor,
                    start = Offset(oneNinthWidth, oneThirdHeight),
                    end = Offset(oneNinthWidth, midHeight),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                // Bottom vertical line
                drawLine(
                    color = lineColor,
                    start = Offset(oneNinthWidth, midHeight),
                    end = Offset(oneNinthWidth, twoThirdsHeight),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceAround
        ) {
            // Here, we determine the appropriate units (meters/kilometers and feet/miles)
            // based on the calculated distance in meters.

            var metricUnits = "m"
            var metricDistance = horizontalLineWidthMeters
            if (horizontalLineWidthMeters > METERS_IN_KILOMETER) {
                // Switch from meters to kilometers as unit
                metricUnits = "km"
                metricDistance /= METERS_IN_KILOMETER.toInt()
            }

            var imperialUnits = "ft"
            var imperialDistance = horizontalLineWidthMeters.toDouble().toFeet()
            if (imperialDistance > FEET_IN_MILE) {
                // Switch from ft to miles as unit
                imperialUnits = "mi"
                imperialDistance = imperialDistance.toMiles()
            }

            // We display the calculated distances in two Text composables, one for imperial
            // and one for metric units.
            ScaleText(
                modifier = Modifier.align(End),
                textColor = textColor,
                shadowColor = shadowColor,
                text = "${imperialDistance.toInt()} $imperialUnits"
            )
            ScaleText(
                modifier = Modifier.align(End),
                textColor = textColor,
                shadowColor = shadowColor,
                text = "$metricDistance $metricUnits"
            )
        }
    }
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
) {
    val visible = remember {
        MutableTransitionState(true)
    }

    // `LaunchedEffect` is a coroutine-based effect that is launched when the composable
    // enters the composition. The `key1` parameter is used to re-launch the effect
    // whenever the value of the key changes. In this case, we are using
    // `cameraPositionState.position` as the key, so the effect will be re-launched
    // every time the camera position changes.
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
        exit = exitTransition
    ) {
        ScaleBar(
            width = width,
            height = height,
            cameraPositionState = cameraPositionState,
            textColor = textColor,
            lineColor = lineColor,
            shadowColor = shadowColor
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
                blurRadius = 1f
            )
        )
    )
}

/**
 * Converts [this] value in meters to the corresponding value in feet.
 * This is a utility function used for unit conversion.
 * @return [this] meters value converted to feet
 */
internal fun Double.toFeet(): Double {
    return this * CENTIMETERS_IN_METER / CENTIMETERS_IN_INCH / INCHES_IN_FOOT
}

/**
 * Converts [this] value in feet to the corresponding value in miles.
 * This is a utility function used for unit conversion.
 * @return [this] feet value converted to miles
 */
internal fun Double.toMiles(): Double {
    return this / FEET_IN_MILE
}

private const val CENTIMETERS_IN_METER: Double = 100.0
private const val METERS_IN_KILOMETER: Double = 1000.0
private const val CENTIMETERS_IN_INCH: Double = 2.54
private const val INCHES_IN_FOOT: Double = 12.0
private const val FEET_IN_MILE: Double = 5280.0