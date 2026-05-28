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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

/**
 * Demonstrates the minimum configuration to initialize a basic, interactive Google Map.
 *
 * This Composable initializes a standard map viewport and manages its camera position state using
 * [rememberCameraPositionState].
 */
@Composable
fun BasicMapSnippet() {
  // [START maps_android_compose_init_basic]
  val cameraPositionState = rememberCameraPositionState {
    position =
      CameraPosition.fromLatLngZoom(
        LatLng(40.0150, -105.2705), // Boulder, Colorado
        11f
      )
  }

  GoogleMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState)
  // [END maps_android_compose_init_basic]
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
  val cameraPositionState = rememberCameraPositionState { position = defaultCameraPosition }

  var configStep by remember { mutableIntStateOf(0) }

  // Automatically cycle through 3 different configurations every 2 seconds to capture transition
  // details
  LaunchedEffect(Unit) {
    while (true) {
      delay(2000.milliseconds)
      configStep = (configStep + 1) % 3
    }
  }

  // Dynamically derive MapProperties based on the current state index
  val properties =
    remember(configStep) {
      when (configStep) {
        0 -> MapProperties(mapType = MapType.SATELLITE, isTrafficEnabled = false)
        1 -> MapProperties(mapType = MapType.TERRAIN, isTrafficEnabled = true)
        else -> MapProperties(mapType = MapType.NORMAL, isTrafficEnabled = false)
      }
    }

  // Dynamically derive MapUiSettings based on the current state index
  val uiSettings =
    remember(configStep) {
      when (configStep) {
        0 -> MapUiSettings(compassEnabled = true, zoomControlsEnabled = false)
        1 -> MapUiSettings(compassEnabled = false, zoomControlsEnabled = true)
        else -> MapUiSettings(compassEnabled = true, zoomControlsEnabled = false)
      }
    }

  GoogleMap(
    modifier = Modifier.fillMaxSize(),
    cameraPositionState = cameraPositionState,
    properties = properties,
    uiSettings = uiSettings
  )
  // [END maps_android_compose_init_custom]
}
