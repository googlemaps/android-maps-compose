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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
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
 * Implement your own observer on camera move events using [CameraPositionState] and pass it in
 * as [cameraPositionState].
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
    Box(
        modifier = modifier
            .size(width = width, height = height)
    ) {
        var horizontalLineWidthMeters by remember {
            mutableStateOf(0)
        }

        Canvas(
            modifier = Modifier.fillMaxSize(),
            onDraw = {
                // Get width of canvas in meters
                val upperLeftLatLng =
                    cameraPositionState.projection?.fromScreenLocation(Point(0, 0))
                        ?: LatLng(0.0, 0.0)
                val upperRightLatLng =
                    cameraPositionState.projection?.fromScreenLocation(Point(0, size.width.toInt()))
                        ?: LatLng(0.0, 0.0)
                val canvasWidthMeters = upperLeftLatLng.sphericalDistance(upperRightLatLng)
                val eightNinthsCanvasMeters = (canvasWidthMeters * 8 / 9).toInt()

                horizontalLineWidthMeters = eightNinthsCanvasMeters

                val oneNinthWidth = size.width / 9
                val midHeight = size.height / 2
                val oneThirdHeight = size.height / 3
                val twoThirdsHeight = size.height * 2 / 3
                val strokeWidth = 4f
                val shadowStrokeWidth = strokeWidth + 3

                // Middle horizontal line shadow (drawn under main lines)
                drawLine(
                    color = shadowColor,
                    start = Offset(oneNinthWidth, midHeight),
                    end = Offset(size.width, midHeight),
                    strokeWidth = shadowStrokeWidth,
                    cap = StrokeCap.Round
                )
                // Top vertical line shadow (drawn under main lines)
                drawLine(
                    color = shadowColor,
                    start = Offset(oneNinthWidth, oneThirdHeight),
                    end = Offset(oneNinthWidth, midHeight),
                    strokeWidth = shadowStrokeWidth,
                    cap = StrokeCap.Round
                )
                // Bottom vertical line shadow (drawn under main lines)
                drawLine(
                    color = shadowColor,
                    start = Offset(oneNinthWidth, midHeight),
                    end = Offset(oneNinthWidth, twoThirdsHeight),
                    strokeWidth = shadowStrokeWidth,
                    cap = StrokeCap.Round
                )

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
 * Implement your own observer on camera move events using [CameraPositionState] and pass it in
 * as [cameraPositionState].
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

    LaunchedEffect(key1 = cameraPositionState.position.zoom) {
        // Show ScaleBar
        visible.targetState = true
        delay(visibilityDurationMillis.toLong())
        // Hide ScaleBar after timeout period
        visible.targetState = false
    }

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
 * Converts [this] value in meters to the corresponding value in feet
 * @return [this] meters value converted to feet
 */
internal fun Double.toFeet(): Double {
    return this * CENTIMETERS_IN_METER / CENTIMETERS_IN_INCH / INCHES_IN_FOOT
}

/**
 * Converts [this] value in feet to the corresponding value in miles
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