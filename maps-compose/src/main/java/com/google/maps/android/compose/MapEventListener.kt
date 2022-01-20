package com.google.maps.android.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.maps.model.IndoorBuilding
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest

/**
 * Holder class for top-level click listeners.
 */
internal class MapClickListeners(
    indoorStateChangeListener: IndoorStateChangeListener = DefaultIndoorStateChangeListener,
    listener: MapEventListener = DefaultMapEventListeners,
) {
    var mapEventListener: MapEventListener by mutableStateOf(listener)
    var indoorStateChangeListener: IndoorStateChangeListener by
        mutableStateOf(indoorStateChangeListener)
}

/**
 * Default implementation of [MapEventListener] with no-op implementations.
 */
object DefaultMapEventListeners: MapEventListener

/**
 * Default implementation of [IndoorStateChangeListener] with no-op
 * implementations.
 */
object DefaultIndoorStateChangeListener: IndoorStateChangeListener

/**
 * Interface definition for building indoor level state changes.
 */
interface IndoorStateChangeListener {
    /**
     * Callback invoked when an indoor building comes to focus.
     */
    fun onIndoorBuildingFocused() { }

    /**
     * Callback invoked when a level for a building is activated.
     * @param building the activated building
     */
    fun onIndoorLevelActivated(building: IndoorBuilding) { }
}

/**
 * Interface definition for user and rendering-related events that occur on the
 * map.
 */
interface MapEventListener {
    /**
     * Invoked when the map is clicked.
     *
     * @param latLng the [LatLng] clicked
     */
    fun onMapClick(latLng: LatLng) { }

    /**
     * Invoked when the map is long clicked.
     *
     * @param latLng the [LatLng] long clicked
     */
    fun onMapLongClick(latLng: LatLng) { }

    /**
     * Invoked when the map has finished rendering.
     */
    fun onMapLoaded() { }

    /**
     * Invoked when the my location button is clicked.
     *
     * @return true if the listeners consumed the event, false otherwise. If the
     * listener does not consume the event, then the default behavior of moving
     * the camera such that it is centered on the user location will occur.
     */
    fun onMyLocationButtonClick(): Boolean { return false }

    /**
     * Invoked when the my location blue dot is clicked.
     */
    fun onMyLocationClick() { }

    /**
     * Invoked when a point of interest on the map is clicked.
     *
     * @param poi the clicked point of interest
     */
    fun onPOIClick(poi: PointOfInterest) { }
}