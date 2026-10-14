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
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * Demonstrates the minimum configuration to initialize a basic, interactive Google Map.
 *
 * This Composable initializes a standard map viewport and manages its camera position state using
 * [rememberCameraPositionState].
 */
@Composable
fun BasicMapSnippet(onStateConfigured: (CameraPositionState) -> Unit = {}) {
  // [START maps_android_compose_init_basic]
  val boulder = LatLng(40.0150, -105.2705)
  val cameraPositionState = rememberCameraPositionState {
    position = CameraPosition.fromLatLngZoom(boulder, 11f)
  }

  GoogleMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState)
  // [END maps_android_compose_init_basic]

  // Expose active camera state to test listeners
  onStateConfigured(cameraPositionState)
}

/**
 * Demonstrates how to initialize a Google Map with custom properties and UI controls.
 *
 * This Composable configures the map to use a [MapType.SATELLITE] layer and customizes the UI
 * settings to enable the compass while hiding the default zoom control buttons.
 */
@Composable
fun CustomConfigMapSnippet() {
  // [START maps_android_compose_init_custom]
  val singapore = LatLng(1.3588, 103.8742)
  val cameraPositionState = rememberCameraPositionState {
    position = CameraPosition.fromLatLngZoom(singapore, 11f)
  }

  // Configure custom map features and layers
  val mapProperties = remember {
    MapProperties(mapType = MapType.SATELLITE, isTrafficEnabled = true)
  }

  // Configure UI gestures and built-in controls
  val mapUiSettings = remember {
    MapUiSettings(
      compassEnabled = true,
      zoomControlsEnabled = false,
      myLocationButtonEnabled = true
    )
  }

  GoogleMap(
    modifier = Modifier.fillMaxSize(),
    cameraPositionState = cameraPositionState,
    properties = mapProperties,
    uiSettings = mapUiSettings
  )
  // [END maps_android_compose_init_custom]
}
