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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import cocoapods.GoogleMaps.GMSCameraPosition
import cocoapods.GoogleMaps.GMSMapStyle
import cocoapods.GoogleMaps.GMSMapView
import cocoapods.GoogleMaps.kGMSTypeHybrid
import cocoapods.GoogleMaps.kGMSTypeNone
import cocoapods.GoogleMaps.kGMSTypeNormal
import cocoapods.GoogleMaps.kGMSTypeSatellite
import cocoapods.GoogleMaps.kGMSTypeTerrain
import com.google.maps.android.model.CameraPosition
import com.google.maps.android.model.LatLng
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero

@OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)
@Composable
internal actual fun PlatformGoogleMap(
    modifier: Modifier,
    cameraPositionState: CameraPositionState,
    properties: MapProperties,
    uiSettings: MapUiSettings,
    onMapClick: (LatLng) -> Unit,
    onMapLongClick: (LatLng) -> Unit,
    onMapLoaded: () -> Unit,
    registry: MapNodeRegistry,
) {
    val currentOnMapClick by rememberUpdatedState(onMapClick)
    val currentOnMapLongClick by rememberUpdatedState(onMapLongClick)
    val currentOnMapLoaded by rememberUpdatedState(onMapLoaded)

    val mapView = remember {
        GMSMapView(frame = CGRectZero.readValue(), camera = cameraPositionState.position.toGms())
    }
    val renderer = remember(mapView, registry) { MapNodeRenderer(mapView, registry) }
    // GMSMapView holds its delegate weakly, so keep a strong reference here.
    val delegate = remember(mapView, renderer, cameraPositionState) {
        MapViewDelegate(
            cameraPositionState = cameraPositionState,
            renderer = renderer,
            onMapClick = { currentOnMapClick(it) },
            onMapLongClick = { currentOnMapLongClick(it) },
            onMapLoaded = { currentOnMapLoaded() },
        )
    }

    DisposableEffect(mapView, delegate) {
        mapView.delegate = delegate
        onDispose { mapView.delegate = null }
    }
    DisposableEffect(mapView, cameraPositionState) {
        // The map may have been moved before it was shown, for example by a saved state.
        mapView.camera = cameraPositionState.position.toGms()
        cameraPositionState.controller = MapViewCameraController(mapView)
        onDispose { cameraPositionState.controller = null }
    }
    DisposableEffect(renderer) {
        renderer.start()
        onDispose { renderer.stop() }
    }

    UIKitView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            view.applyProperties(properties)
            view.applyUiSettings(uiSettings, properties.isMyLocationEnabled)
        },
        properties = UIKitInteropProperties(
            // The map handles its own gestures; do not let Compose delay or intercept them.
            interactionMode = UIKitInteropInteractionMode.NonCooperative,
        ),
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun GMSMapView.applyProperties(properties: MapProperties) {
    mapType = when (properties.mapType) {
        MapType.NONE -> kGMSTypeNone
        MapType.NORMAL -> kGMSTypeNormal
        MapType.SATELLITE -> kGMSTypeSatellite
        MapType.TERRAIN -> kGMSTypeTerrain
        MapType.HYBRID -> kGMSTypeHybrid
    }
    myLocationEnabled = properties.isMyLocationEnabled
    buildingsEnabled = properties.isBuildingEnabled
    trafficEnabled = properties.isTrafficEnabled
    indoorEnabled = properties.isIndoorEnabled
    mapStyle = properties.mapStyleJson?.let { GMSMapStyle.styleWithJSONString(it, error = null) }
    setMinZoom(properties.minZoomPreference, maxZoom = properties.maxZoomPreference)
}

@OptIn(ExperimentalForeignApi::class)
private fun GMSMapView.applyUiSettings(uiSettings: MapUiSettings, isMyLocationEnabled: Boolean) {
    settings.compassButton = uiSettings.compassEnabled
    // As on Android, the button only shows while the user's location does.
    settings.myLocationButton = uiSettings.myLocationButtonEnabled && isMyLocationEnabled
    settings.rotateGestures = uiSettings.rotationGesturesEnabled
    settings.scrollGestures = uiSettings.scrollGesturesEnabled
    settings.tiltGestures = uiSettings.tiltGesturesEnabled
    settings.zoomGestures = uiSettings.zoomGesturesEnabled
}

@OptIn(ExperimentalForeignApi::class)
internal fun CameraPosition.toGms(): GMSCameraPosition = GMSCameraPosition(
    target = target.toCoordinate(),
    zoom = zoom,
    bearing = bearing.toDouble(),
    viewingAngle = tilt.toDouble(),
)

@OptIn(ExperimentalForeignApi::class)
internal fun GMSCameraPosition.toShared(): CameraPosition = CameraPosition(
    target = target.toLatLng(),
    zoom = zoom,
    tilt = viewingAngle.toFloat(),
    bearing = bearing.toFloat(),
)
