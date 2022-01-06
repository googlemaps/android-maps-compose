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

import android.os.Parcelable
import androidx.annotation.Px
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import kotlinx.parcelize.Parcelize

@Parcelize
data class Padding(
    @Px val leftPx: Int = 0,
    @Px val topPx: Int = 0,
    @Px val rightPx: Int = 0,
    @Px val bottomPx: Int = 0,
) : Parcelable

/**
 * State object for setting properties on the map.
 */
@Stable
class MapPropertiesState(
    contentDescription: String? = "Google Map",
    isBuildingEnabled: Boolean = false,
    isIndoorEnabled: Boolean = false,
    isMyLocationEnabled: Boolean = false,
    isTrafficEnabled: Boolean = false,
    latLngBoundsForCameraTarget: LatLngBounds? = null,
    mapStyleOptions: MapStyleOptions? = null,
    mapType: MapType = MapType.NORMAL,
    maxZoomPreference: Float = 21.0f,
    minZoomPreference: Float = 3.0f,
    padding: Padding = Padding()
) {
    /**
     * The content description for the map used by accessibility services to describe the map.
     * @see com.google.android.gms.maps.GoogleMap.setContentDescription
     */
    var contentDescription: String? by mutableStateOf(contentDescription)

    /**
     * Boolean indicating if buildings are enabled.
     */
    var isBuildingEnabled: Boolean by mutableStateOf(isBuildingEnabled)

    /**
     * Boolean indicating if indoor maps are enabled.
     * @see  com.google.android.gms.maps.GoogleMap.isIndoorEnabled
     */
    var isIndoorEnabled: Boolean by mutableStateOf(isIndoorEnabled)

    /**
     * Boolean indicating if the my-location layer should be enabled. Before setting this property
     * to 'true', ensure that `ACCESS_COARSE_LOCATION` or `ACCESS_FINE_LOCATION` permissions have
     * been granted.
     * @see com.google.android.gms.maps.GoogleMap.setMyLocationEnabled
     */
    var isMyLocationEnabled: Boolean by mutableStateOf(isMyLocationEnabled)

    /**
     * Boolean indicating if the traffic layer is on or off.
     * @see com.google.android.gms.maps.GoogleMap.setTrafficEnabled
     */
    var isTrafficEnabled: Boolean by mutableStateOf(isTrafficEnabled)

    /**
     * A [LatLngBounds] to constrain the camera target.
     * @see com.google.android.gms.maps.GoogleMap.setLatLngBoundsForCameraTarget
     */
    var latLngBoundsForCameraTarget: LatLngBounds? by mutableStateOf(latLngBoundsForCameraTarget)

    /**
     * The styling options for the map.
     * @see com.google.android.gms.maps.GoogleMap.setMapStyle
     */
    var mapStyleOptions: MapStyleOptions? by mutableStateOf(mapStyleOptions)

    /**
     * The type of map tiles that should be displayed.
     * @see com.google.android.gms.maps.GoogleMap.getMapType
     */
    var mapType: MapType by mutableStateOf(mapType)

    /**
     * The preferred upper bound for the camera zoom.
     * @see com.google.android.gms.maps.GoogleMap.getMaxZoomLevel
     */
    var maxZoomPreference: Float by mutableStateOf(maxZoomPreference)

    /**
     * The preferred lower bound for the camera zoom.
     * @see com.google.android.gms.maps.GoogleMap.getMinZoomLevel
     */
    var minZoomPreference: Float by mutableStateOf(minZoomPreference)

    /**
     * The padding on the map.
     * @see com.google.android.gms.maps.GoogleMap.setPadding
     */
    var padding: Padding by mutableStateOf(padding)

    companion object {
        val Saver = run {
            val contentDescriptionKey = "ContentDescriptionKey"
            val isBuildingEnabledKey = "IsBuildingEnabledKey"
            val isIndoorEnabledKey = "IsIndoorEnabledKey"
            val isMyLocationEnabledKey = "IsIndoorEnabledKey"
            val isTrafficEnabledKey = "IsTrafficEnabledKey"
            val latLngBoundsForCameraTargetKey = "LatLngBoundsForCameraTargetKey"
            val mapStyleOptionsKey = "MapStyleOptionsKey"
            val mapTypeKey = "MapTypeKey"
            val maxZoomPreferenceKey = "MaxZoomPreferenceKey"
            val minZoomPreferenceKey = "MinZoomPreferenceKey"
            val paddingKey = "PaddingKey"
            mapSaver(
                save = {
                    mapOf(
                        contentDescriptionKey to it.contentDescription,
                        isBuildingEnabledKey to it.isBuildingEnabled,
                        isIndoorEnabledKey to it.isIndoorEnabled,
                        isMyLocationEnabledKey to it.isMyLocationEnabled,
                        isTrafficEnabledKey to it.isTrafficEnabled,
                        latLngBoundsForCameraTargetKey to it.latLngBoundsForCameraTarget,
                        mapStyleOptionsKey to it.mapStyleOptions,
                        mapTypeKey to it.mapType,
                        maxZoomPreferenceKey to it.maxZoomPreference,
                        minZoomPreferenceKey to it.minZoomPreference,
                        paddingKey to it.padding,
                    )
                },
                restore = {
                    MapPropertiesState(
                        contentDescription = it[contentDescriptionKey] as String?,
                        isBuildingEnabled = it[isBuildingEnabledKey] as Boolean,
                        isIndoorEnabled = it[isIndoorEnabledKey] as Boolean,
                        isMyLocationEnabled = it[isMyLocationEnabledKey] as Boolean,
                        isTrafficEnabled = it[isTrafficEnabledKey] as Boolean,
                        latLngBoundsForCameraTarget = it[latLngBoundsForCameraTargetKey] as LatLngBounds?,
                        mapStyleOptions = it[mapStyleOptionsKey] as MapStyleOptions?,
                        mapType = it[mapTypeKey] as MapType,
                        maxZoomPreference = it[maxZoomPreferenceKey] as Float,
                        minZoomPreference = it[minZoomPreferenceKey] as Float,
                        padding = it[paddingKey] as Padding,
                    )
                }
            )
        }
    }
}

/**
 * Remembers a [MapPropertiesState] that is remembered across compositions and configuration.
 *
 * @param init an optional lambda for providing initial values to the [MapPropertiesState]
 */
@Composable
fun rememberMapPropertiesState(
    init: MapPropertiesState.() -> Unit = {},
): MapPropertiesState =
    rememberSaveable(saver = MapPropertiesState.Saver) {
        MapPropertiesState().apply(init)
    }