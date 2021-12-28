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