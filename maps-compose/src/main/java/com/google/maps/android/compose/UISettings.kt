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