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

package com.google.maps.android.compose.demo

import android.graphics.Point
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.widgets.DarkGray
import com.google.maps.android.compose.widgets.DisappearingScaleBar
import com.google.maps.android.compose.widgets.ScaleBar
import com.google.maps.android.sphericalDistance
import com.google.maps.android.withSphericalOffset

private val scaleBarWidth = 65.dp

class ScaleBarActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isMapLoaded by remember { mutableStateOf(false) }
            var measureAtCenter by rememberSaveable { mutableStateOf(true) }
            var showCrosshairs by rememberSaveable { mutableStateOf(true) }

            // To control and observe the map camera
            val cameraPositionState = rememberCameraPositionState {
                position = if (intent.hasExtra("lat") && intent.hasExtra("lng")) {
                    CameraPosition.fromLatLngZoom(
                        LatLng(
                            intent.getFloatExtra("lat", defaultCameraPosition.target.latitude.toFloat()).toDouble(),
                            intent.getFloatExtra("lng", defaultCameraPosition.target.longitude.toFloat()).toDouble(),
                        ),
                        intent.getFloatExtra("zoom", defaultCameraPosition.zoom),
                    )
                } else {
                    defaultCameraPosition
                }
            }

            val scaleBackground = MaterialTheme.colors.background.copy(alpha = 0.4f)
            val controlsBackground = MaterialTheme.colors.background.copy(alpha = 0.7f)
            val scaleBorderStroke = BorderStroke(width = 1.dp, DarkGray.copy(alpha = 0.2f))

            Box(
                modifier = Modifier.fillMaxSize()
                    .systemBarsPadding(),
            ) {
                GoogleMap(
                    modifier = Modifier.matchParentSize(),
                    cameraPositionState = cameraPositionState,
                    onMapLoaded = {
                        isMapLoaded = true
                    },
                ) {
                    if (showCrosshairs) {
                        ScaleBarReferencePolylines(
                            cameraPositionState = cameraPositionState,
                            measureAtCenter = measureAtCenter,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(top = 5.dp, start = 5.dp)
                        .align(Alignment.TopStart)
                        .background(
                            scaleBackground,
                            shape = MaterialTheme.shapes.medium,
                        )
                        .border(
                            scaleBorderStroke,
                            shape = MaterialTheme.shapes.medium,
                        ),
                ) {
                    DisappearingScaleBar(
                        modifier = Modifier.padding(end = 4.dp),
                        cameraPositionState = cameraPositionState,
                        measureAtCenter = measureAtCenter,
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(top = 5.dp, end = 5.dp)
                        .align(Alignment.TopEnd)
                        .background(
                            scaleBackground,
                            shape = MaterialTheme.shapes.medium,
                        )
                        .border(
                            scaleBorderStroke,
                            shape = MaterialTheme.shapes.medium,
                        ),
                ) {
                    ScaleBar(
                        modifier = Modifier.padding(end = 4.dp),
                        cameraPositionState = cameraPositionState,
                        measureAtCenter = measureAtCenter,
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(start = 8.dp, bottom = 40.dp)
                        .align(Alignment.BottomStart)
                        .background(
                            controlsBackground,
                            shape = MaterialTheme.shapes.medium,
                        )
                        .border(
                            scaleBorderStroke,
                            shape = MaterialTheme.shapes.medium,
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Row(
                        modifier = Modifier.clickable { measureAtCenter = !measureAtCenter },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = measureAtCenter,
                            onCheckedChange = { measureAtCenter = it },
                        )
                        Text(
                            text = "Measure at center",
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                    Row(
                        modifier = Modifier.clickable { showCrosshairs = !showCrosshairs },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = showCrosshairs,
                            onCheckedChange = { showCrosshairs = it },
                        )
                        Text(
                            text = "Show crosshairs",
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                }

                if (!isMapLoaded) {
                    AnimatedVisibility(
                        modifier = Modifier
                            .matchParentSize(),
                        visible = !isMapLoaded,
                        enter = EnterTransition.None,
                        exit = fadeOut(),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .background(MaterialTheme.colors.background)
                                .wrapContentSize(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Draws three sets of crossed reference polylines (at 10%, 50%, and 90% of the viewport height)
 * aligned with geographic East–West (`270°`–`90°`, red) and North–South (`180°`–`0°`, blue), where
 * every line's ground length on the Earth's surface matches the [ScaleBar]'s horizontal bar
 * distance at the camera target (`position.target`).
 *
 * Because Google Maps uses a conformal Web Mercator projection (`EPSG:3857`), map scale stretches
 * isotropically (in both East–West and North–South directions) by a factor of `1 / cos(latitude)`
 * as latitude moves from the Equator toward either pole:
 * - At the **middle cross** (`50%` height, anchored at `position.target`), the ground distance of
 *   both arms matches the exact pixel width of the [ScaleBar]'s horizontal line.
 * - At the **top (`10%`)** and **bottom (`90%`)** crosses, the exact same ground distance in meters
 *   appears visibly larger in screen pixels when closer to the poles (higher `|latitude|`) and
 *   smaller in screen pixels when closer to the Equator.
 */
@Composable
@GoogleMapComposable
private fun ScaleBarReferencePolylines(cameraPositionState: CameraPositionState, measureAtCenter: Boolean = true) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val referenceCrosses by remember(cameraPositionState, density, configuration, measureAtCenter) {
        derivedStateOf {
            val position = cameraPositionState.position
            cameraPositionState.isMoving
            val projection = cameraPositionState.projection ?: return@derivedStateOf emptyList()
            val widthInPixels = with(density) { scaleBarWidth.toPx().toInt() }
            val centerPoint = projection.toScreenLocation(position.target)
            if (centerPoint.x <= 0 || centerPoint.y <= 0) return@derivedStateOf emptyList()

            val startPoint: Point
            val endPoint: Point
            if (measureAtCenter) {
                val halfWidthInPixels = widthInPixels / 2
                startPoint = Point(centerPoint.x - halfWidthInPixels, centerPoint.y)
                endPoint = Point(startPoint.x + widthInPixels, centerPoint.y)
            } else {
                startPoint = Point(0, 0)
                endPoint = Point(widthInPixels, 0)
            }
            val startLatLng = projection.fromScreenLocation(startPoint)
            val endLatLng = projection.fromScreenLocation(endPoint)
            val scaleBarMeters = (startLatLng.sphericalDistance(endLatLng) * 8 / 9).toInt().toDouble()
            if (scaleBarMeters <= 0.0) return@derivedStateOf emptyList()

            val halfDistanceMeters = scaleBarMeters / 2.0
            val topAnchor = projection.fromScreenLocation(Point(centerPoint.x, centerPoint.y / 5))
            val middleAnchor = position.target
            val bottomAnchor = projection.fromScreenLocation(Point(centerPoint.x, centerPoint.y * 9 / 5))

            listOf(topAnchor, middleAnchor, bottomAnchor).map { anchor ->
                val west = anchor.withSphericalOffset(halfDistanceMeters, 270.0)
                val east = anchor.withSphericalOffset(halfDistanceMeters, 90.0)
                val south = anchor.withSphericalOffset(halfDistanceMeters, 180.0)
                val north = anchor.withSphericalOffset(halfDistanceMeters, 0.0)
                listOf(west, east) to listOf(south, north)
            }
        }
    }

    referenceCrosses.forEach { (eastWestPoints, northSouthPoints) ->
        Polyline(
            points = eastWestPoints,
            color = Color(0xFFD93025),
            width = 6f,
        )
        Polyline(
            points = northSouthPoints,
            color = Color(0xFF1A73E8),
            width = 6f,
        )
    }
}
