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

import androidx.annotation.ColorInt
import androidx.compose.runtime.Stable
import com.google.android.gms.maps.GoogleMapOptions

/**
 * This class contains a subset of properties that are set in [GoogleMapOptions] that can only be
 * provided during map initialization.
 *
 * @param ambientEnabled whether ambient-mode styling should be enabled
 * @param backgroundColor the map background color
 * @param liteMode whether the map should be created in lite mode
 * @param mapId the map's ID
 * @param zOrderOnTop whether the map view's surface is placed on top of its window
 */
@Stable
data class MapOptions(
    val ambientEnabled: Boolean = false,
    @ColorInt val backgroundColor: Int? = null,
    val liteMode: Boolean = false,
    val mapId: String? = null,
    val zOrderOnTop: Boolean? = null
)

internal fun MapOptions.toGoogleMapOptions() : GoogleMapOptions =
    GoogleMapOptions().also { googleMapOptions ->
        googleMapOptions.ambientEnabled(ambientEnabled)
        backgroundColor?.let { googleMapOptions.backgroundColor(it) }
        googleMapOptions.liteMode(liteMode)
        mapId?.let { googleMapOptions.mapId(it) }
        zOrderOnTop?.let { googleMapOptions.zOrderOnTop(it) }
    }