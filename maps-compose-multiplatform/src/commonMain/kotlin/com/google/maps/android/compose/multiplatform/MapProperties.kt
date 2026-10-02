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

import androidx.compose.runtime.Immutable

/** The type of map tiles to display. */
public enum class MapType {
    NONE,
    NORMAL,
    SATELLITE,
    TERRAIN,
    HYBRID,
}

/**
 * Properties of the map content, as opposed to the controls in [MapUiSettings].
 *
 * @param mapType the type of map tiles
 * @param isMyLocationEnabled whether to show the user's location. The app must hold the location
 * permission on Android and the location authorization on iOS.
 * @param isBuildingEnabled whether to show 3D buildings
 * @param isTrafficEnabled whether to show traffic
 * @param isIndoorEnabled whether to show indoor maps
 * @param mapStyleJson a JSON map style, see https://developers.google.com/maps/documentation/android-sdk/styling.
 * Ignored for maps with a map ID.
 * @param minZoomPreference the lowest zoom the camera can reach
 * @param maxZoomPreference the highest zoom the camera can reach
 */
@Immutable
public class MapProperties(
    public val mapType: MapType = MapType.NORMAL,
    public val isMyLocationEnabled: Boolean = false,
    public val isBuildingEnabled: Boolean = false,
    public val isTrafficEnabled: Boolean = false,
    public val isIndoorEnabled: Boolean = false,
    public val mapStyleJson: String? = null,
    public val minZoomPreference: Float = 3f,
    public val maxZoomPreference: Float = 21f,
) {
    public fun copy(
        mapType: MapType = this.mapType,
        isMyLocationEnabled: Boolean = this.isMyLocationEnabled,
        isBuildingEnabled: Boolean = this.isBuildingEnabled,
        isTrafficEnabled: Boolean = this.isTrafficEnabled,
        isIndoorEnabled: Boolean = this.isIndoorEnabled,
        mapStyleJson: String? = this.mapStyleJson,
        minZoomPreference: Float = this.minZoomPreference,
        maxZoomPreference: Float = this.maxZoomPreference,
    ): MapProperties = MapProperties(
        mapType, isMyLocationEnabled, isBuildingEnabled, isTrafficEnabled, isIndoorEnabled,
        mapStyleJson, minZoomPreference, maxZoomPreference,
    )

    private val fields: List<Any?>
        get() = listOf(
            mapType, isMyLocationEnabled, isBuildingEnabled, isTrafficEnabled, isIndoorEnabled,
            mapStyleJson, minZoomPreference, maxZoomPreference,
        )

    override fun equals(other: Any?): Boolean = other is MapProperties && fields == other.fields

    override fun hashCode(): Int = fields.hashCode()

    override fun toString(): String = "MapProperties$fields"
}

/**
 * The map's built-in controls and gestures.
 *
 * Android also has zoom buttons, which stay hidden so that both platforms look the same.
 */
@Immutable
public class MapUiSettings(
    public val compassEnabled: Boolean = true,
    public val myLocationButtonEnabled: Boolean = true,
    public val rotationGesturesEnabled: Boolean = true,
    public val scrollGesturesEnabled: Boolean = true,
    public val tiltGesturesEnabled: Boolean = true,
    public val zoomGesturesEnabled: Boolean = true,
) {
    public fun copy(
        compassEnabled: Boolean = this.compassEnabled,
        myLocationButtonEnabled: Boolean = this.myLocationButtonEnabled,
        rotationGesturesEnabled: Boolean = this.rotationGesturesEnabled,
        scrollGesturesEnabled: Boolean = this.scrollGesturesEnabled,
        tiltGesturesEnabled: Boolean = this.tiltGesturesEnabled,
        zoomGesturesEnabled: Boolean = this.zoomGesturesEnabled,
    ): MapUiSettings = MapUiSettings(
        compassEnabled, myLocationButtonEnabled, rotationGesturesEnabled, scrollGesturesEnabled,
        tiltGesturesEnabled, zoomGesturesEnabled,
    )

    private val fields: List<Any?>
        get() = listOf(
            compassEnabled, myLocationButtonEnabled, rotationGesturesEnabled, scrollGesturesEnabled,
            tiltGesturesEnabled, zoomGesturesEnabled,
        )

    override fun equals(other: Any?): Boolean = other is MapUiSettings && fields == other.fields

    override fun hashCode(): Int = fields.hashCode()

    override fun toString(): String = "MapUiSettings$fields"
}
