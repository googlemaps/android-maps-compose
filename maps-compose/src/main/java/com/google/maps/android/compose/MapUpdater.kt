// Copyright 2022 Google LLC
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.LocationSource
import com.google.android.gms.maps.model.IndoorBuilding

internal class MapPropertiesNode(
    val map: GoogleMap,
    cameraPositionState: CameraPositionState,
    contentDescription: String?,
    var clickListeners: MapClickListeners,
    var density: Density,
    var layoutDirection: LayoutDirection,
) : MapNode {

    init {
        cameraPositionState.setMap(map)
        if (contentDescription != null) {
            map.setContentDescription(contentDescription)
        }
    }

    var contentDescription = contentDescription
        set(value) {
            field = value
            map.setContentDescription(contentDescription)
        }

    var cameraPositionState = cameraPositionState
        set(value) {
            if (value == field) return
            field.setMap(null)
            field = value
            value.setMap(map)
        }

    override fun onAttached() {
        map.setOnCameraIdleListener {
            cameraPositionState.isMoving = false
            // setOnCameraMoveListener is only invoked when the camera position
            // is changed via .animate(). To handle updating state when .move()
            // is used, it's necessary to set the camera's position here as well
            cameraPositionState.rawPosition = map.cameraPosition
        }
        map.setOnCameraMoveCanceledListener {
            cameraPositionState.isMoving = false
        }
        map.setOnCameraMoveStartedListener {
            cameraPositionState.cameraMoveStartedReason = CameraMoveStartedReason.fromInt(it)
            cameraPositionState.isMoving = true
        }
        map.setOnCameraMoveListener {
            cameraPositionState.rawPosition = map.cameraPosition
        }
        map.setOnMapClickListener { clickListeners.onMapClick(it) }
        map.setOnMapLongClickListener { clickListeners.onMapLongClick(it) }
        map.setOnMapLoadedCallback { clickListeners.onMapLoaded() }
        map.setOnMyLocationButtonClickListener { clickListeners.onMyLocationButtonClick() }
        map.setOnMyLocationClickListener { clickListeners.onMyLocationClick(it) }
        map.setOnPoiClickListener { clickListeners.onPOIClick(it) }
        map.setOnIndoorStateChangeListener(object : GoogleMap.OnIndoorStateChangeListener {
            override fun onIndoorBuildingFocused() {
                clickListeners.indoorStateChangeListener.onIndoorBuildingFocused()
            }

            override fun onIndoorLevelActivated(building: IndoorBuilding) {
                clickListeners.indoorStateChangeListener.onIndoorLevelActivated(building)
            }
        })
    }

    override fun onRemoved() {
        cameraPositionState.setMap(null)
    }

    override fun onCleared() {
        cameraPositionState.setMap(null)
    }
}

internal val NoPadding = PaddingValues()

/**
 * Used to keep the primary map properties up to date. This should never leave the map composition.
 */
@SuppressLint("MissingPermission")
@Suppress("NOTHING_TO_INLINE")
@Composable
internal inline fun MapUpdater(
    contentDescription: String?,
    cameraPositionState: CameraPositionState,
    clickListeners: MapClickListeners,
    contentPadding: PaddingValues = NoPadding,
    locationSource: LocationSource?,
    mapProperties: MapProperties,
    mapUiSettings: MapUiSettings,
) {
    val map = (currentComposer.applier as MapApplier).map
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    ComposeNode<MapPropertiesNode, MapApplier>(
        factory = {
            MapPropertiesNode(
                map = map,
                contentDescription = contentDescription,
                cameraPositionState = cameraPositionState,
                clickListeners = clickListeners,
                density = density,
                layoutDirection = layoutDirection,
            )
        }
    ) {
        // The node holds density and layoutDirection so that the updater blocks can be
        // non-capturing, allowing the compiler to turn them into singletons
        update(density) { this.density = it }
        update(layoutDirection) { this.layoutDirection = it }
        update(contentDescription) { this.contentDescription = it }

        set(locationSource) { map.setLocationSource(it) }
        set(mapProperties.isBuildingEnabled) { map.isBuildingsEnabled = it }
        set(mapProperties.isIndoorEnabled) { map.isIndoorEnabled = it }
        set(mapProperties.isMyLocationEnabled) { map.isMyLocationEnabled = it }
        set(mapProperties.isTrafficEnabled) { map.isTrafficEnabled = it }
        set(mapProperties.latLngBoundsForCameraTarget) { map.setLatLngBoundsForCameraTarget(it) }
        set(mapProperties.mapStyleOptions) { map.setMapStyle(it) }
        set(mapProperties.mapType) { map.mapType = it.value }
        set(mapProperties.maxZoomPreference) { map.setMaxZoomPreference(it) }
        set(mapProperties.minZoomPreference) { map.setMinZoomPreference(it) }
        set(contentPadding) {
            val node = this
            with(this.density) {
                map.setPadding(
                    it.calculateLeftPadding(node.layoutDirection).roundToPx(),
                    it.calculateTopPadding().roundToPx(),
                    it.calculateRightPadding(node.layoutDirection).roundToPx(),
                    it.calculateBottomPadding().roundToPx()
                )
            }
        }

        set(mapUiSettings.compassEnabled) { map.uiSettings.isCompassEnabled = it }
        set(mapUiSettings.indoorLevelPickerEnabled) { map.uiSettings.isIndoorLevelPickerEnabled = it }
        set(mapUiSettings.mapToolbarEnabled) { map.uiSettings.isMapToolbarEnabled = it }
        set(mapUiSettings.myLocationButtonEnabled) { map.uiSettings.isMyLocationButtonEnabled = it }
        set(mapUiSettings.rotationGesturesEnabled) { map.uiSettings.isRotateGesturesEnabled = it }
        set(mapUiSettings.scrollGesturesEnabled) { map.uiSettings.isScrollGesturesEnabled = it }
        set(mapUiSettings.scrollGesturesEnabledDuringRotateOrZoom) { map.uiSettings.isScrollGesturesEnabledDuringRotateOrZoom = it }
        set(mapUiSettings.tiltGesturesEnabled) { map.uiSettings.isTiltGesturesEnabled = it }
        set(mapUiSettings.zoomControlsEnabled) { map.uiSettings.isZoomControlsEnabled = it }
        set(mapUiSettings.zoomGesturesEnabled) { map.uiSettings.isZoomGesturesEnabled = it }

        update(cameraPositionState) { this.cameraPositionState = it }
        update(clickListeners) { this.clickListeners = it }
    }
}
