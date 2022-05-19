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

import java.util.Objects

internal val DefaultMapUiSettings = MapUiSettings()

/**
 * Data class for UI-related settings on the map.
 *
 * Note: This is intentionally a class and not a data class for binary
 * compatibility on future changes.
 * See: https://jakewharton.com/public-api-challenges-in-kotlin/
 */
public class MapUiSettings(
    public val compassEnabled: Boolean = true,
    public val indoorLevelPickerEnabled: Boolean = true,
    public val mapToolbarEnabled: Boolean = true,
    public val myLocationButtonEnabled: Boolean = true,
    public val rotationGesturesEnabled: Boolean = true,
    public val scrollGesturesEnabled: Boolean = true,
    public val scrollGesturesEnabledDuringRotateOrZoom: Boolean = true,
    public val tiltGesturesEnabled: Boolean = true,
    public val zoomControlsEnabled: Boolean = true,
    public val zoomGesturesEnabled: Boolean = true,
) {
    override fun toString(): String = "MapUiSettings(" +
        "compassEnabled=$compassEnabled, indoorLevelPickerEnabled=$indoorLevelPickerEnabled, " +
        "mapToolbarEnabled=$mapToolbarEnabled, myLocationButtonEnabled=$myLocationButtonEnabled, " +
        "rotationGesturesEnabled=$rotationGesturesEnabled, scrollGesturesEnabled=$scrollGesturesEnabled, " +
        "scrollGesturesEnabledDuringRotateOrZoom=$scrollGesturesEnabledDuringRotateOrZoom, " +
        "tiltGesturesEnabled=$tiltGesturesEnabled, zoomControlsEnabled=$zoomControlsEnabled, " +
        "zoomGesturesEnabled=$zoomGesturesEnabled)"

    override fun equals(other: Any?): Boolean = other is MapUiSettings &&
        compassEnabled == other.compassEnabled &&
        indoorLevelPickerEnabled == other.indoorLevelPickerEnabled &&
        mapToolbarEnabled == other.mapToolbarEnabled &&
        myLocationButtonEnabled == other.myLocationButtonEnabled &&
        rotationGesturesEnabled == other.rotationGesturesEnabled &&
        scrollGesturesEnabled == other.scrollGesturesEnabled &&
        scrollGesturesEnabledDuringRotateOrZoom == other.scrollGesturesEnabledDuringRotateOrZoom &&
        tiltGesturesEnabled == other.tiltGesturesEnabled &&
        zoomControlsEnabled == other.zoomControlsEnabled &&
        zoomGesturesEnabled == other.zoomGesturesEnabled

    override fun hashCode(): Int = Objects.hash(
        compassEnabled,
        indoorLevelPickerEnabled,
        mapToolbarEnabled,
        myLocationButtonEnabled,
        rotationGesturesEnabled,
        scrollGesturesEnabled,
        scrollGesturesEnabledDuringRotateOrZoom,
        tiltGesturesEnabled,
        zoomControlsEnabled,
        zoomGesturesEnabled
    )

    public fun copy(
        compassEnabled: Boolean = this.compassEnabled,
        indoorLevelPickerEnabled: Boolean = this.indoorLevelPickerEnabled,
        mapToolbarEnabled: Boolean = this.mapToolbarEnabled,
        myLocationButtonEnabled: Boolean = this.myLocationButtonEnabled,
        rotationGesturesEnabled: Boolean = this.rotationGesturesEnabled,
        scrollGesturesEnabled: Boolean = this.scrollGesturesEnabled,
        scrollGesturesEnabledDuringRotateOrZoom: Boolean = this.scrollGesturesEnabledDuringRotateOrZoom,
        tiltGesturesEnabled: Boolean = this.tiltGesturesEnabled,
        zoomControlsEnabled: Boolean = this.zoomControlsEnabled,
        zoomGesturesEnabled: Boolean = this.zoomGesturesEnabled
    ): MapUiSettings = MapUiSettings(
        compassEnabled = compassEnabled,
        indoorLevelPickerEnabled = indoorLevelPickerEnabled,
        mapToolbarEnabled = mapToolbarEnabled,
        myLocationButtonEnabled = myLocationButtonEnabled,
        rotationGesturesEnabled = rotationGesturesEnabled,
        scrollGesturesEnabled = scrollGesturesEnabled,
        scrollGesturesEnabledDuringRotateOrZoom = scrollGesturesEnabledDuringRotateOrZoom,
        tiltGesturesEnabled = tiltGesturesEnabled,
        zoomControlsEnabled = zoomControlsEnabled,
        zoomGesturesEnabled = zoomGesturesEnabled
    )
}