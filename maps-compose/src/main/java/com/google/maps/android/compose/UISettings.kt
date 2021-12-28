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

@Composable
internal fun UISettings(
    mapView: MapView,
    uiSettingsState: UISettingsState
) {
    val context = LocalContext.current
    LaunchedEffect(
        context,
        mapView,
        uiSettingsState.compassEnabled,
        uiSettingsState.indoorLevelPickerEnabled,
        uiSettingsState.mapToolbarEnabled,
        uiSettingsState.myLocationButtonEnabled,
        uiSettingsState.rotationGesturesEnabled,
        uiSettingsState.scrollGesturesEnabled,
        uiSettingsState.scrollGesturesEnabledDuringRotateOrZoom,
        uiSettingsState.tiltGesturesEnabled,
        uiSettingsState.zoomControlsEnabled,
        uiSettingsState.zoomGesturesEnabled,
    ) {
        val map = mapView.awaitMap()
        map.uiSettings.isCompassEnabled = uiSettingsState.compassEnabled
        map.uiSettings.isIndoorLevelPickerEnabled = uiSettingsState.indoorLevelPickerEnabled
        map.uiSettings.isMapToolbarEnabled = uiSettingsState.mapToolbarEnabled
        map.uiSettings.isMyLocationButtonEnabled = uiSettingsState.myLocationButtonEnabled
        map.uiSettings.isRotateGesturesEnabled = uiSettingsState.rotationGesturesEnabled
        map.uiSettings.isScrollGesturesEnabled = uiSettingsState.scrollGesturesEnabled
        map.uiSettings.isScrollGesturesEnabledDuringRotateOrZoom = uiSettingsState.scrollGesturesEnabledDuringRotateOrZoom
        map.uiSettings.isTiltGesturesEnabled = uiSettingsState.tiltGesturesEnabled
        map.uiSettings.isZoomControlsEnabled = uiSettingsState.zoomControlsEnabled
        map.uiSettings.isZoomGesturesEnabled = uiSettingsState.zoomGesturesEnabled
    }
}