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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerInfoWindowComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

/**
 * Demonstrates how to add a standard Google Map Marker with a title and info snippet.
 *
 * This snippet uses [rememberUpdatedMarkerState] to manage the marker's coordinates and places a
 * standard red pin on the map.
 */
@Composable
fun BasicMarkerSnippet() {
  // [START maps_android_compose_marker_basic]
  val cameraPositionState = rememberCameraPositionState { position = defaultCameraPosition }
  val markerState = rememberUpdatedMarkerState(position = singapore)

  GoogleMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState) {
    Marker(state = markerState, title = "Singapore", snippet = "A beautiful sunny island")
  }
  // [END maps_android_compose_marker_basic]
}

/**
 * Demonstrates how to customize a marker's icon using [BitmapDescriptorFactory].
 *
 * This Composable changes the default red pin color to a default azure color by passing the icon
 * property. Useful for categorized marker configurations.
 */
@Composable
fun CustomMarkerIconSnippet() {
  // [START maps_android_compose_marker_custom_icon]
  val cameraPositionState = rememberCameraPositionState { position = defaultCameraPosition }
  val markerState = rememberUpdatedMarkerState(position = singapore)

  GoogleMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState) {
    Marker(
      state = markerState,
      title = "Singapore Custom Icon",
      // Customizes the marker icon (e.g., azure default pin)
      icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
      // For a custom drawable asset, use:
      // icon = BitmapDescriptorFactory.fromResource(R.drawable.my_custom_marker)
    )
  }
  // [END maps_android_compose_marker_custom_icon]
}

/**
 * Demonstrates how to render arbitrary Jetpack Compose UI as an interactive marker on the map.
 *
 * This snippet leverages [MarkerComposable] to replace the standard marker pin with a styled
 * Compose [Box] containing a [Text] layout. This allows complete, programmatic visual flexibility
 * for markers.
 */
@Composable
fun MarkerComposableSnippet() {
  // [START maps_android_compose_marker_composable]
  val cameraPositionState = rememberCameraPositionState { position = defaultCameraPosition }
  val markerState = rememberUpdatedMarkerState(position = singapore)

  GoogleMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState) {
    // Renders arbitrary Compose UI directly as the map pin
    MarkerComposable(
      state = markerState,
      title = "Compose Marker",
      keys = arrayOf("singapore_composable")
    ) {
      Box(
        modifier =
          Modifier.width(88.dp).height(36.dp).clip(RoundedCornerShape(16.dp)).background(Color.Red),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "Compose UI",
          color = Color.White,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
  // [END maps_android_compose_marker_composable]
}

/**
 * Demonstrates how to customize the balloon InfoWindow popup using arbitrary Compose UI.
 *
 * This Composable uses [MarkerInfoWindowComposable] to customize the popup content rendered when
 * the user taps the marker. The balloon is styled using a yellow [Box] with a black [Text] label.
 */
@Composable
fun CustomInfoWindowSnippet() {
  // [START maps_android_compose_marker_info_window]
  val cameraPositionState = rememberCameraPositionState { position = defaultCameraPosition }
  val markerState = rememberUpdatedMarkerState(position = singapore)

  GoogleMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState) {
    // Custom Info Window popup rendered fully in Compose
    MarkerInfoWindowComposable(
      state = markerState,
      title = "Marker Info Window",
      infoContent = { marker ->
        // Custom pop-up content inside the balloon frame
        Box(
          modifier =
            Modifier.width(150.dp)
              .height(50.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(Color.Yellow),
          contentAlignment = Alignment.Center
        ) {
          Text(text = marker.title ?: "Title", color = Color.Black)
        }
      }
    ) {
      // The marker pin representation itself (a simple blue circle Composable)
      Box(
        modifier =
          Modifier.width(32.dp).height(32.dp).clip(RoundedCornerShape(16.dp)).background(Color.Blue)
      )
    }
  }
  // [END maps_android_compose_marker_info_window]
}
