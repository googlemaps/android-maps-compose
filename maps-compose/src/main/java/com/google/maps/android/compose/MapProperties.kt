package com.google.maps.android.compose

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.LocationSource
import com.google.android.gms.maps.model.IndoorBuilding
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest

internal class MapPropertiesNode(
    val map: GoogleMap,
    cameraPositionState: CameraPositionState,
    var clickListeners: MapClickListeners,
    var density: Density,
    var layoutDirection: LayoutDirection,
) : MapNode {

    init {
        cameraPositionState.setMap(map)
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
        }
        map.setOnCameraMoveCanceledListener {
            cameraPositionState.isMoving = false
        }
        map.setOnCameraMoveStartedListener {
            cameraPositionState.isMoving = true
        }
        map.setOnCameraMoveListener {
            cameraPositionState.rawPosition = map.cameraPosition
        }
        map.setOnMapClickListener { clickListeners.onMapClick(it) }
        map.setOnMapLongClickListener { clickListeners.onMapLongClick(it) }
        map.setOnMapLoadedCallback { clickListeners.onMapLoaded() }
        map.setOnMyLocationButtonClickListener { clickListeners.onMyLocationButtonClick() }
        map.setOnMyLocationClickListener { clickListeners.onMyLocationClick() }
        map.setOnPoiClickListener { clickListeners.onPOIClick(it) }
        map.setOnIndoorStateChangeListener(object : GoogleMap.OnIndoorStateChangeListener {
            override fun onIndoorBuildingFocused() {
                clickListeners.onIndoorBuildingFocused()
            }

            override fun onIndoorLevelActivated(building: IndoorBuilding) {
                clickListeners.onIndoorLevelActivated(building)
            }
        })
    }

    override fun onRemoved() {
        cameraPositionState.setMap(null)
    }
}

private val NoPadding = PaddingValues()

/**
 * Used to keep the primary map properties up to date. This should never leave the map composition.
 */
@SuppressLint("MissingPermission")
@Suppress("NOTHING_TO_INLINE")
@Composable
internal inline fun MapProperties(
    mapPropertiesState: MapPropertiesState,
    uiSettingsState: UISettingsState,
    cameraPositionState: CameraPositionState,
    clickListeners: MapClickListeners,
    locationSource: LocationSource?,
    contentPadding: PaddingValues = NoPadding
) {
    val map = (currentComposer.applier as MapApplier).map
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    ComposeNode<MapPropertiesNode, MapApplier>(
        factory = {
            MapPropertiesNode(
                map = map,
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

        set(locationSource) { map.setLocationSource(it) }
        set(mapPropertiesState.contentDescription) { map.setContentDescription(it) }
        set(mapPropertiesState.isBuildingEnabled) { map.isBuildingsEnabled = it }
        set(mapPropertiesState.isIndoorEnabled) { map.isIndoorEnabled = it }
        set(mapPropertiesState.isMyLocationEnabled) { map.isMyLocationEnabled = it }
        set(mapPropertiesState.isTrafficEnabled) { map.isTrafficEnabled = it }
        set(mapPropertiesState.latLngBoundsForCameraTarget) { map.setLatLngBoundsForCameraTarget(it) }
        set(mapPropertiesState.mapStyleOptions) { map.setMapStyle(it) }
        set(mapPropertiesState.mapType) { map.mapType = it.value }
        set(mapPropertiesState.maxZoomPreference) { map.setMaxZoomPreference(it) }
        set(mapPropertiesState.minZoomPreference) { map.setMinZoomPreference(it) }
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

        set(uiSettingsState.compassEnabled) { map.uiSettings.isCompassEnabled = it }
        set(uiSettingsState.indoorLevelPickerEnabled) { map.uiSettings.isIndoorLevelPickerEnabled = it }
        set(uiSettingsState.mapToolbarEnabled) { map.uiSettings.isMapToolbarEnabled = it }
        set(uiSettingsState.myLocationButtonEnabled) { map.uiSettings.isMyLocationButtonEnabled = it }
        set(uiSettingsState.rotationGesturesEnabled) { map.uiSettings.isRotateGesturesEnabled = it }
        set(uiSettingsState.scrollGesturesEnabled) { map.uiSettings.isScrollGesturesEnabled = it }
        set(uiSettingsState.scrollGesturesEnabledDuringRotateOrZoom) { map.uiSettings.isScrollGesturesEnabledDuringRotateOrZoom = it }
        set(uiSettingsState.tiltGesturesEnabled) { map.uiSettings.isTiltGesturesEnabled = it }
        set(uiSettingsState.zoomControlsEnabled) { map.uiSettings.isZoomControlsEnabled = it }
        set(uiSettingsState.zoomGesturesEnabled) { map.uiSettings.isZoomGesturesEnabled = it }

        update(cameraPositionState) { this.cameraPositionState = it }
        update(clickListeners) { this.clickListeners = it }
    }
}

/**
 * Holder class for top-level click listeners.
 * TODO: Combine/group some of these
 */
internal class MapClickListeners {
    var onIndoorBuildingFocused: () -> Unit by mutableStateOf({})
    var onIndoorLevelActivated: (IndoorBuilding) -> Unit by mutableStateOf({})
    var onMapClick: (LatLng) -> Unit by mutableStateOf({})
    var onMapLongClick: (LatLng) -> Unit by mutableStateOf({})
    var onMapLoaded: () -> Unit by mutableStateOf({})
    var onMyLocationButtonClick: () -> Boolean by mutableStateOf({ false })
    var onMyLocationClick: () -> Unit by mutableStateOf({})
    var onPOIClick: (PointOfInterest) -> Unit by mutableStateOf({})
}
