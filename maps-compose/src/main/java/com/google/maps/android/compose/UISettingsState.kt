package com.google.maps.android.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * State object for setting UI-related settings on the map.
 */
@Stable
class UISettingsState(
    compassEnabled: Boolean,
    indoorLevelPickerEnabled: Boolean,
    mapToolbarEnabled: Boolean,
    myLocationButtonEnabled: Boolean,
    rotationGesturesEnabled: Boolean,
    scrollGesturesEnabled: Boolean,
    scrollGesturesEnabledDuringRotateOrZoom: Boolean,
    tiltGesturesEnabled: Boolean,
    zoomControlsEnabled: Boolean,
    zoomGesturesEnabled: Boolean,
) {
    /**
     * Whether the compass should be enabled/disabled.
     */
    var compassEnabled: Boolean by mutableStateOf(compassEnabled)

    /**
     * Whether the indoor level picker should be enabled/disabled.
     */
    var indoorLevelPickerEnabled: Boolean by mutableStateOf(indoorLevelPickerEnabled)

    /**
     * Whether the map toolbar should be enabled/disabled.
     */
    var mapToolbarEnabled: Boolean by mutableStateOf(mapToolbarEnabled)

    /**
     * Whether the my location button should be enabled/disabled.
     */
    var myLocationButtonEnabled: Boolean by mutableStateOf(myLocationButtonEnabled)

    /**
     * Whether rotation gestures should be enabled/disabled.
     */
    var rotationGesturesEnabled: Boolean by mutableStateOf(rotationGesturesEnabled)

    /**
     * Whether scroll gestures should be enabled/disabled.
     */
    var scrollGesturesEnabled: Boolean by mutableStateOf(scrollGesturesEnabled)

    /**
     * Whether scroll gestures should be enabled/disabled during rotation or zoom.
     */
    var scrollGesturesEnabledDuringRotateOrZoom: Boolean by mutableStateOf(scrollGesturesEnabledDuringRotateOrZoom)

    /**
     * Whether tilt gestures should be enabled/disabled.
     */
    var tiltGesturesEnabled: Boolean by mutableStateOf(tiltGesturesEnabled)

    /**
     * Whether zoom controls should be enabled/disabled.
     */
    var zoomControlsEnabled: Boolean by mutableStateOf(zoomControlsEnabled)

    /**
     * Whether zoom gestures should be enabled/disabled.
     */
    var zoomGesturesEnabled: Boolean by mutableStateOf(zoomGesturesEnabled)

    companion object {
        val Saver = run {
            val compassEnabledKey = "CompassEnabledKey"
            val indoorLevelPickerEnabledKey = "IndoorLevelPickerEnabledKey"
            val mapToolbarEnabledKey = "MapToolbarEnabledKey"
            val myLocationButtonEnabledKey = "MyLocationButtonEnabledKey"
            val rotationGesturesEnabledKey = "RotationGesturesEnabledKey"
            val scrollGesturesEnabledKey = "ScrollGesturesEnabledKey"
            val scrollGesturesEnabledDuringRotateOrZoomKey = "ScrollGesturesEnabledDuringRotateOrZoomKey"
            val tiltGesturesEnabledKey = "TiltGesturesEnabledKey"
            val zoomControlsEnabledKey = "ZoomControlsEnabledKey"
            val zoomGesturesEnabledKey = "ZoomGesturesEnabledKey"
            mapSaver(
                save = {
                    mapOf(
                        compassEnabledKey to it.compassEnabled,
                        indoorLevelPickerEnabledKey to it.indoorLevelPickerEnabled,
                        mapToolbarEnabledKey to it.mapToolbarEnabled,
                        myLocationButtonEnabledKey to it.myLocationButtonEnabled,
                        rotationGesturesEnabledKey to it.rotationGesturesEnabled,
                        scrollGesturesEnabledKey to it.scrollGesturesEnabled,
                        scrollGesturesEnabledDuringRotateOrZoomKey to it.scrollGesturesEnabledDuringRotateOrZoom,
                        tiltGesturesEnabledKey to it.tiltGesturesEnabled,
                        zoomControlsEnabledKey to it.zoomControlsEnabled,
                        zoomGesturesEnabledKey to it.zoomGesturesEnabled,
                    )
                },
                restore = {
                    UISettingsState(
                        compassEnabled = it[compassEnabledKey] as Boolean,
                        indoorLevelPickerEnabled = it[indoorLevelPickerEnabledKey] as Boolean,
                        mapToolbarEnabled = it[mapToolbarEnabledKey] as Boolean,
                        myLocationButtonEnabled = it[myLocationButtonEnabledKey] as Boolean,
                        rotationGesturesEnabled = it[rotationGesturesEnabledKey] as Boolean,
                        scrollGesturesEnabled = it[scrollGesturesEnabledKey] as Boolean,
                        scrollGesturesEnabledDuringRotateOrZoom = it[scrollGesturesEnabledDuringRotateOrZoomKey] as Boolean,
                        tiltGesturesEnabled = it[tiltGesturesEnabledKey] as Boolean,
                        zoomControlsEnabled = it[zoomControlsEnabledKey] as Boolean,
                        zoomGesturesEnabled = it[zoomGesturesEnabledKey] as Boolean,
                    )
                }
            )
        }
    }
}

@Composable
fun rememberUISettingsState(
    initialCompassEnabled: Boolean = true,
    initialIndoorLevelPickerEnabled: Boolean = true,
    initialMapToolbarEnabled: Boolean = true,
    initialMyLocationButtonEnabled: Boolean = true,
    initialRotationGesturesEnabled: Boolean = true,
    initialScrollGesturesEnabled: Boolean = true,
    initialScrollGesturesEnabledDuringRotateOrZoom: Boolean = true,
    initialTiltGesturesEnabled: Boolean = true,
    initialZoomControlsEnabled: Boolean = true,
    initialZoomGesturesEnabled: Boolean = true,
) : UISettingsState =
    rememberSaveable(saver = UISettingsState.Saver) {
        UISettingsState(
            compassEnabled =  initialCompassEnabled,
            indoorLevelPickerEnabled = initialIndoorLevelPickerEnabled,
            mapToolbarEnabled = initialMapToolbarEnabled,
            myLocationButtonEnabled = initialMyLocationButtonEnabled,
            rotationGesturesEnabled = initialRotationGesturesEnabled,
            scrollGesturesEnabled = initialScrollGesturesEnabled,
            scrollGesturesEnabledDuringRotateOrZoom = initialScrollGesturesEnabledDuringRotateOrZoom,
            tiltGesturesEnabled = initialTiltGesturesEnabled,
            zoomControlsEnabled = initialZoomControlsEnabled,
            zoomGesturesEnabled = initialZoomGesturesEnabled,
        )
    }