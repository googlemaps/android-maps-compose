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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.Circle as MapsComposeCircle
import com.google.maps.android.compose.GoogleMap as MapsComposeGoogleMap
import com.google.maps.android.compose.MapProperties as MapsComposeMapProperties
import com.google.maps.android.compose.MapType as MapsComposeMapType
import com.google.maps.android.compose.MapUiSettings as MapsComposeMapUiSettings
import com.google.maps.android.compose.Marker as MapsComposeMarker
import com.google.maps.android.compose.MarkerState as MapsComposeMarkerState
import com.google.maps.android.compose.Polygon as MapsComposePolygon
import com.google.maps.android.compose.Polyline as MapsComposePolyline
import com.google.maps.android.compose.rememberCameraPositionState as rememberMapsComposeCameraPositionState
import com.google.maps.android.model.CameraPosition
import com.google.maps.android.model.LatLng

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
    // The shared CameraPosition is the Play services class on Android, so it passes through.
    val mapCameraState = rememberMapsComposeCameraPositionState {
        position = cameraPositionState.position
    }
    DisposableEffect(cameraPositionState, mapCameraState) {
        cameraPositionState.controller = object : CameraController {
            override fun move(position: CameraPosition) {
                mapCameraState.move(CameraUpdateFactory.newCameraPosition(position))
            }

            override suspend fun animate(position: CameraPosition, durationMs: Int) {
                mapCameraState.animate(CameraUpdateFactory.newCameraPosition(position), durationMs)
            }
        }
        onDispose { cameraPositionState.controller = null }
    }
    LaunchedEffect(cameraPositionState, mapCameraState) {
        snapshotFlow { mapCameraState.position to mapCameraState.isMoving }
            .collect { (position, isMoving) -> cameraPositionState.onCameraChanged(position, isMoving) }
    }

    val mapProperties = remember(properties) { properties.toMapsCompose() }
    val mapUiSettings = remember(uiSettings) { uiSettings.toMapsCompose() }

    MapsComposeGoogleMap(
        modifier = modifier,
        cameraPositionState = mapCameraState,
        properties = mapProperties,
        uiSettings = mapUiSettings,
        onMapClick = onMapClick,
        onMapLongClick = onMapLongClick,
        onMapLoaded = onMapLoaded,
    ) {
        registry.nodes.forEach { node ->
            key(node) {
                when (node) {
                    is MarkerNode -> MarkerNodeContent(node)
                    is PolylineNode -> MapsComposePolyline(
                        points = node.points,
                        color = node.color,
                        width = node.width.dpToPx(),
                        geodesic = node.geodesic,
                        clickable = node.clickable,
                        visible = node.visible,
                        zIndex = node.zIndex,
                        onClick = { node.onClick() },
                    )
                    is PolygonNode -> MapsComposePolygon(
                        points = node.points,
                        holes = node.holes,
                        fillColor = node.fillColor,
                        strokeColor = node.strokeColor,
                        strokeWidth = node.strokeWidth.dpToPx(),
                        geodesic = node.geodesic,
                        clickable = node.clickable,
                        visible = node.visible,
                        zIndex = node.zIndex,
                        onClick = { node.onClick() },
                    )
                    is CircleNode -> MapsComposeCircle(
                        center = node.center,
                        radius = node.radius,
                        fillColor = node.fillColor,
                        strokeColor = node.strokeColor,
                        strokeWidth = node.strokeWidth.dpToPx(),
                        clickable = node.clickable,
                        visible = node.visible,
                        zIndex = node.zIndex,
                        onClick = { node.onClick() },
                    )
                }
            }
        }
    }
}

/**
 * maps-compose keeps its own MarkerState, so mirror the shared one into it, and mirror drags
 * back. Both directions skip equal positions, so they do not echo each other.
 */
@Composable
private fun MarkerNodeContent(node: MarkerNode) {
    val sharedState = node.state
    val mapState = remember(sharedState) { MapsComposeMarkerState(sharedState.position) }
    LaunchedEffect(sharedState, mapState) {
        snapshotFlow { sharedState.position }.collect {
            if (mapState.position != it) mapState.position = it
        }
    }
    LaunchedEffect(sharedState, mapState) {
        snapshotFlow { mapState.position }.collect {
            if (sharedState.position != it) sharedState.position = it
        }
    }
    MapsComposeMarker(
        state = mapState,
        title = node.title,
        snippet = node.snippet,
        icon = remember(node.color) { node.color?.let { BitmapDescriptorFactory.defaultMarker(it.hue()) } },
        alpha = node.alpha,
        draggable = node.draggable,
        flat = node.flat,
        rotation = node.rotation,
        visible = node.visible,
        zIndex = node.zIndex,
        onClick = { node.onClick(sharedState) },
    )
}

private fun MapProperties.toMapsCompose() = MapsComposeMapProperties(
    isBuildingEnabled = isBuildingEnabled,
    isIndoorEnabled = isIndoorEnabled,
    isMyLocationEnabled = isMyLocationEnabled,
    isTrafficEnabled = isTrafficEnabled,
    mapStyleOptions = mapStyleJson?.let { MapStyleOptions(it) },
    mapType = when (mapType) {
        MapType.NONE -> MapsComposeMapType.NONE
        MapType.NORMAL -> MapsComposeMapType.NORMAL
        MapType.SATELLITE -> MapsComposeMapType.SATELLITE
        MapType.TERRAIN -> MapsComposeMapType.TERRAIN
        MapType.HYBRID -> MapsComposeMapType.HYBRID
    },
    maxZoomPreference = maxZoomPreference,
    minZoomPreference = minZoomPreference,
)

private fun MapUiSettings.toMapsCompose() = MapsComposeMapUiSettings(
    compassEnabled = compassEnabled,
    myLocationButtonEnabled = myLocationButtonEnabled,
    rotationGesturesEnabled = rotationGesturesEnabled,
    scrollGesturesEnabled = scrollGesturesEnabled,
    tiltGesturesEnabled = tiltGesturesEnabled,
    zoomGesturesEnabled = zoomGesturesEnabled,
    // iOS has no zoom buttons; hide them so both platforms look the same.
    zoomControlsEnabled = false,
)

/** The hue, in degrees, that tints Android's default marker to this color. */
private fun Color.hue(): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    return hsv[0]
}

/** Widths are density-independent in the shared API; maps-compose takes pixels. */
@Composable
private fun Float.dpToPx(): Float = with(LocalDensity.current) { this@dpToPx.dp.toPx() }
