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

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.LocationSource
import com.google.android.gms.maps.MapView
import com.google.maps.android.ktx.awaitMap

/**
 * Composable that sets the properties on a map given [mapPropertiesState].
 */
@SuppressLint("MissingPermission")
@Composable
internal fun MapProperties(
    mapView: MapView,
    mapPropertiesState: MapPropertiesState,
    locationSource: LocationSource?
) {
    val context = LocalContext.current
    LaunchedEffect(
        context,
        mapView,
        locationSource,
        mapPropertiesState.contentDescription,
        mapPropertiesState.isBuildingEnabled,
        mapPropertiesState.isIndoorEnabled,
        mapPropertiesState.isMyLocationEnabled,
        mapPropertiesState.isTrafficEnabled,
        mapPropertiesState.latLngBoundsForCameraTarget,
        mapPropertiesState.mapStyleOptions,
        mapPropertiesState.mapType,
        mapPropertiesState.minZoomPreference,
        mapPropertiesState.maxZoomPreference,
        mapPropertiesState.padding,
    ) {
        val map = mapView.awaitMap()
        map.setLocationSource(locationSource)
        map.setContentDescription(mapPropertiesState.contentDescription)
        map.isBuildingsEnabled = mapPropertiesState.isBuildingEnabled
        map.isIndoorEnabled = mapPropertiesState.isIndoorEnabled
        map.isMyLocationEnabled = mapPropertiesState.isMyLocationEnabled
        map.isTrafficEnabled = mapPropertiesState.isTrafficEnabled
        map.setLatLngBoundsForCameraTarget(mapPropertiesState.latLngBoundsForCameraTarget)
        map.setMapStyle(mapPropertiesState.mapStyleOptions)
        map.mapType = mapPropertiesState.mapType.value
        map.setMaxZoomPreference(mapPropertiesState.maxZoomPreference)
        map.setMinZoomPreference(mapPropertiesState.minZoomPreference)
        map.setPadding(
            mapPropertiesState.padding.leftPx,
            mapPropertiesState.padding.topPx,
            mapPropertiesState.padding.rightPx,
            mapPropertiesState.padding.bottomPx,
        )
    }
}