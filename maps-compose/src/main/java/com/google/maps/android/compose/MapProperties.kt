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
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions

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
        map.setOnMapClickListener {
            clickListeners.mapEventListener.onMapClick(it)
        }
        map.setOnMapLongClickListener {
            clickListeners.mapEventListener.onMapLongClick(it)
        }
        map.setOnMapLoadedCallback {
            clickListeners.mapEventListener.onMapLoaded()
        }
        map.setOnMyLocationButtonClickListener {
            clickListeners.mapEventListener.onMyLocationButtonClick()
        }
        map.setOnMyLocationClickListener {
            clickListeners.mapEventListener.onMyLocationClick()
        }
        map.setOnPoiClickListener {
            clickListeners.mapEventListener.onPOIClick(it)
        }
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
}

internal val NoPadding = PaddingValues()

/**
 * Used to keep the primary map properties up to date. This should never leave the map composition.
 */
@SuppressLint("MissingPermission")
@Suppress("NOTHING_TO_INLINE")
@Composable
internal inline fun MapProperties(
    cameraPositionState: CameraPositionState,
    clickListeners: MapClickListeners,
    contentPadding: PaddingValues = NoPadding,
    locationSource: LocationSource?,
    mapPropertiesHolder: MapPropertiesHolder,
    mapUiSettings: MapUiSettings,
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
        set(mapPropertiesHolder.contentDescription) { map.setContentDescription(it) }
        set(mapPropertiesHolder.isBuildingEnabled) { map.isBuildingsEnabled = it }
        set(mapPropertiesHolder.isIndoorEnabled) { map.isIndoorEnabled = it }
        set(mapPropertiesHolder.isMyLocationEnabled) { map.isMyLocationEnabled = it }
        set(mapPropertiesHolder.isTrafficEnabled) { map.isTrafficEnabled = it }
        set(mapPropertiesHolder.latLngBoundsForCameraTarget) { map.setLatLngBoundsForCameraTarget(it) }
        set(mapPropertiesHolder.mapStyleOptions) { map.setMapStyle(it) }
        set(mapPropertiesHolder.mapType) { map.mapType = it.value }
        set(mapPropertiesHolder.maxZoomPreference) { map.setMaxZoomPreference(it) }
        set(mapPropertiesHolder.minZoomPreference) { map.setMinZoomPreference(it) }
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

internal class MapPropertiesHolder(
    contentDescription: String? = null,
    isBuildingEnabled: Boolean = false,
    isIndoorEnabled: Boolean = false,
    isMyLocationEnabled: Boolean = false,
    isTrafficEnabled: Boolean = false,
    latLngBoundsForCameraTarget: LatLngBounds? = null,
    mapStyleOptions: MapStyleOptions? = null,
    mapType: MapType = MapType.NORMAL,
    maxZoomPreference: Float = 21.0f,
    minZoomPreference: Float = 3.0f,
) {
    var contentDescription: String? by mutableStateOf(contentDescription)
    var isBuildingEnabled: Boolean by mutableStateOf(isBuildingEnabled)
    var isIndoorEnabled: Boolean by mutableStateOf(isIndoorEnabled)
    var isMyLocationEnabled: Boolean by mutableStateOf(isMyLocationEnabled)
    var isTrafficEnabled: Boolean by mutableStateOf(isTrafficEnabled)
    var latLngBoundsForCameraTarget: LatLngBounds? by mutableStateOf(latLngBoundsForCameraTarget)
    var mapStyleOptions: MapStyleOptions? by mutableStateOf(mapStyleOptions)
    var mapType: MapType by mutableStateOf(mapType)
    var maxZoomPreference: Float by mutableStateOf(maxZoomPreference)
    var minZoomPreference: Float by mutableStateOf(minZoomPreference)
}