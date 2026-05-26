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

package com.google.maps.android.compose.snippets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

val singapore = LatLng(1.3588227, 103.8742114)
val defaultCameraPosition = CameraPosition.fromLatLngZoom(singapore, 11f)

/**
 * Demonstrates how to move the map camera instantly to a new coordinate and zoom level.
 *
 * This snippet uses `cameraPositionState.move(...)` inside a [LaunchedEffect] to trigger
 * an immediate, non-animated camera relocation once the Composable enters the composition tree.
 */
@Composable
fun MoveCameraSnippet() {
    // [START maps_android_compose_camera_move]
    val cameraPositionState = rememberCameraPositionState {
        position = defaultCameraPosition
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    )

    // Instantly updates the camera position (for example, on load or inside a callback)
    LaunchedEffect(Unit) {
        cameraPositionState.move(
            CameraUpdateFactory.newLatLngZoom(LatLng(1.40, 103.77), 12f)
        )
    }
    // [END maps_android_compose_camera_move]
}

/**
 * Demonstrates how to smoothly animate the map camera to a targeted coordinate and zoom.
 *
 * This Composable shows a button overlaid on the map. Clicks on the button launch a coroutine
 * in the [rememberCoroutineScope] which executes a smooth camera animation via
 * `cameraPositionState.animate(...)` over a specified duration in milliseconds.
 */
@Composable
fun AnimateCameraSnippet() {
    // [START maps_android_compose_camera_animate]
    val cameraPositionState = rememberCameraPositionState {
        position = defaultCameraPosition
    }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        )

        Button(
            onClick = {
                coroutineScope.launch {
                    cameraPositionState.animate(
                        update = CameraUpdateFactory.newLatLngZoom(LatLng(1.40, 103.77), 14f),
                        durationMs = 2000
                    )
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        ) {
            Text("Animate Camera")
        }
    }
    // [END maps_android_compose_camera_animate]
}

/**
 * Demonstrates how to restrict the map camera's movement to a specific geographic bounding box.
 *
 * This snippet configures the map with a [LatLngBounds] restriction passed through [MapProperties],
 * preventing the user from panning or zooming the camera target outside the specified bounds.
 */
@Composable
fun RestrictCameraBoundsSnippet() {
    // [START maps_android_compose_camera_bounds]
    val southwest = LatLng(1.20, 103.60)
    val northeast = LatLng(1.45, 104.05)
    val singaporeBounds = LatLngBounds(southwest, northeast)

    val cameraPositionState = rememberCameraPositionState {
        position = defaultCameraPosition
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        // Restrict camera target bounds inside MapProperties
        properties = com.google.maps.android.compose.MapProperties(
            latLngBoundsForCameraTarget = singaporeBounds
        )
    )
    // [END maps_android_compose_camera_bounds]
}
