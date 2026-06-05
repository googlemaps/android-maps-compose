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

package com.google.maps.android.compose.multiplatform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
public actual fun GoogleMap(
    modifier: Modifier,
    latitude: Double,
    longitude: Double,
    zoom: Float,
    mapType: MapType,
    myLocationEnabled: Boolean,
    scrollGesturesEnabled: Boolean,
    zoomGesturesEnabled: Boolean,
    markers: List<MapMarker>
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(latitude, longitude), zoom)
    }

    val mapProperties = remember(mapType, myLocationEnabled) {
        com.google.maps.android.compose.MapProperties(
            mapType = when (mapType) {
                MapType.NONE -> com.google.maps.android.compose.MapType.NONE
                MapType.NORMAL -> com.google.maps.android.compose.MapType.NORMAL
                MapType.SATELLITE -> com.google.maps.android.compose.MapType.SATELLITE
                MapType.TERRAIN -> com.google.maps.android.compose.MapType.TERRAIN
                MapType.HYBRID -> com.google.maps.android.compose.MapType.HYBRID
            },
            isMyLocationEnabled = myLocationEnabled
        )
    }

    val mapUiSettings = remember(scrollGesturesEnabled, zoomGesturesEnabled) {
        com.google.maps.android.compose.MapUiSettings(
            scrollGesturesEnabled = scrollGesturesEnabled,
            zoomGesturesEnabled = zoomGesturesEnabled
        )
    }

    com.google.maps.android.compose.GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = mapUiSettings
    ) {
        markers.forEach { markerData ->
            val markerState = com.google.maps.android.compose.rememberMarkerState(
                position = LatLng(markerData.latitude, markerData.longitude)
            )
            com.google.maps.android.compose.Marker(
                state = markerState,
                title = markerData.title,
                snippet = markerData.snippet
            )
        }
    }
}

