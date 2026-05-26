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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * Demonstrates how to draw a styled vector path (Polyline) connecting coordinates on the map.
 *
 * This snippet uses [Polyline] inside the [GoogleMap] content block, customizing the line's
 * color to blue and thickness to 10 pixels.
 */
@Composable
fun PolylineSnippet() {
    // [START maps_android_compose_polyline]
    val cameraPositionState = rememberCameraPositionState {
        position = defaultCameraPosition
    }

    val points = remember {
        listOf(
            LatLng(1.35, 103.87),
            LatLng(1.40, 103.77),
            LatLng(1.45, 103.77)
        )
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        Polyline(
            points = points,
            color = Color.Blue,
            width = 10f
        )
    }
    // [END maps_android_compose_polyline]
}

/**
 * Demonstrates how to draw a styled, solid filled vector area (Polygon) on the map.
 *
 * This snippet configures a [Polygon] with a translucent red fill color and a solid red border stroke.
 */
@Composable
fun PolygonSnippet() {
    // [START maps_android_compose_polygon]
    val cameraPositionState = rememberCameraPositionState {
        position = defaultCameraPosition
    }

    val points = remember {
        listOf(
            LatLng(1.35, 103.87),
            LatLng(1.40, 103.77),
            LatLng(1.40, 103.90)
        )
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        Polygon(
            points = points,
            fillColor = Color.Red.copy(alpha = 0.3f),
            strokeColor = Color.Red,
            strokeWidth = 5f
        )
    }
    // [END maps_android_compose_polygon]
}

/**
 * Demonstrates how to draw a styled geographic circle on the map centered at a coordinate.
 *
 * This Composable uses [Circle] with a radius specified in meters (2,000m) and styles it
 * with a translucent green fill and a solid green outline.
 */
@Composable
fun CircleSnippet() {
    // [START maps_android_compose_circle]
    val cameraPositionState = rememberCameraPositionState {
        position = defaultCameraPosition
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        Circle(
            center = singapore,
            radius = 2000.0, // in meters
            fillColor = Color.Green.copy(alpha = 0.2f),
            strokeColor = Color.Green,
            strokeWidth = 4f
        )
    }
    // [END maps_android_compose_circle]
}
