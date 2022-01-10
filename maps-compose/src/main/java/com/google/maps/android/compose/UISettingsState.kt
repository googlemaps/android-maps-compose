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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.google.android.gms.maps.UiSettings

/**
 * State object for setting UI-related settings on the map.
 */
@Stable
class UISettingsState(
    compassEnabled: Boolean = true,
    indoorLevelPickerEnabled: Boolean = true,
    mapToolbarEnabled: Boolean = true,
    myLocationButtonEnabled: Boolean = true,
    rotationGesturesEnabled: Boolean = true,
    scrollGesturesEnabled: Boolean = true,
    scrollGesturesEnabledDuringRotateOrZoom: Boolean = true,
    tiltGesturesEnabled: Boolean = true,
    zoomControlsEnabled: Boolean = true,
    zoomGesturesEnabled: Boolean = true,
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

/**
 * Creates a [UISettingsState] that is remembered across compositions and configurations.
 *
 * @param init an optional lambda for providing initial values to the [UISettingsState]
 */
@Composable
fun rememberUISettingsState(
    init: UISettingsState.() -> Unit = {},
) : UISettingsState =
    rememberSaveable(saver = UISettingsState.Saver) {
        UISettingsState().apply(init)
    }

internal fun UiSettings.applyState(state: UISettingsState) {
    isCompassEnabled = state.compassEnabled
    isIndoorLevelPickerEnabled = state.indoorLevelPickerEnabled
    isMapToolbarEnabled = state.mapToolbarEnabled
    isMyLocationButtonEnabled = state.myLocationButtonEnabled
    isRotateGesturesEnabled = state.rotationGesturesEnabled
    isScrollGesturesEnabled = state.scrollGesturesEnabled
    isScrollGesturesEnabledDuringRotateOrZoom =
        state.scrollGesturesEnabledDuringRotateOrZoom
    isTiltGesturesEnabled = state.tiltGesturesEnabled
    isZoomControlsEnabled = state.zoomControlsEnabled
    isZoomGesturesEnabled = state.zoomGesturesEnabled
}
