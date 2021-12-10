// Copyright 2021 Google LLC
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

package com.google.maps.android.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.MapView
import com.google.maps.android.ktx.awaitMap

/**
 * This effect has two purposes:
 * (1) Responds to camera updates on the [cameraPositionState]
 * (2) Listens to camera events on the map so that those events can be observed via
 * [cameraPositionState]
 */
@Composable
internal fun CameraEffect(mapView: MapView, cameraPositionState: CameraPositionState) {
    val context = LocalContext.current
    val cameraPosition = cameraPositionState.internalCameraUpdate
    LaunchedEffect(key1 = context, key2 = mapView, key3 = cameraPosition, block = {
        val map = mapView.awaitMap()
        if (cameraPositionState.animated) {
            map.animateCamera(cameraPosition)
        } else {
            map.moveCamera(cameraPosition)
        }

        map.setOnCameraIdleListener {
            cameraPositionState.cameraState = MapCameraState.IDLE
            cameraPositionState.cameraPosition = map.cameraPosition
        }
        map.setOnCameraMoveCanceledListener {
            cameraPositionState.cameraState = MapCameraState.MOVE_CANCELED
            cameraPositionState.cameraPosition = map.cameraPosition
        }
        map.setOnCameraMoveStartedListener {
            cameraPositionState.cameraState = MapCameraState.MOVE_STARTED
            cameraPositionState.cameraPosition = map.cameraPosition
        }
        map.setOnCameraMoveListener {
            cameraPositionState.cameraState = MapCameraState.MOVING
            cameraPositionState.cameraPosition = map.cameraPosition
        }
    })
}