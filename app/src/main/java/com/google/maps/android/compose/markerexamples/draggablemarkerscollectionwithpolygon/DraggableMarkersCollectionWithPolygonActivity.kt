// Copyright 2024 Google LLC
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

package com.google.maps.android.compose.markerexamples.draggablemarkerscollectionwithpolygon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.defaultCameraPosition
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.theme.MapsComposeSampleTheme

/**
 * Data type representing a location.
 *
 * This only stores location position, for illustration,
 * but could hold other data related to the location.
 */
@Immutable
private data class LocationData(val position: LatLng)

/**
 * Unique, stable key for location
 */
private class LocationKey

/**
 * Encapsulates mapping from data model to MarkerStates. Part of view model.
 * MarkerStates are relegated to an implementation detail.
 * Use new [DraggableMarkersModel] instance if data model is updated externally:
 * MarkerStates are source of truth after initialization from data model.
 */
@Stable
private class DraggableMarkersModel(dataModel: Map<LocationKey, LocationData>) {
    // This initializes MarkerState from our model once (model is initial source of truth)
    // and never updates it from the model afterwards.
    // See SyncingDraggableMarkerWithDataModelActivity for rationale.
    private val markerDataMap: SnapshotStateMap<LocationKey, MarkerState> = mutableStateMapOf(
        *dataModel.entries.map { (locationKey, locationData) ->
            locationKey to MarkerState(locationData.position)
        }.toTypedArray()
    )

    /** Add new marker location to model */
    fun addLocation(locationData: LocationData) {
        markerDataMap += LocationKey() to MarkerState(locationData.position)
    }

    /** Delete marker location from model */
    private fun deleteLocation(locationKey: LocationKey) {
        markerDataMap -= locationKey
    }

    /**
     * Render Markers from model
     */
    @Composable
    fun Markers() = markerDataMap.forEach { (locationKey, markerState) ->
        key(locationKey) {
            LocationMarker(
                markerState,
                onClick = { deleteLocation(locationKey) }
            )
        }
    }

    /**
     * List of functions providing current positions of Markers.
     *
     * Calling from composition will trigger recomposition when Markers and their positions
     * change.
     */
    val markerPositionsModel: List<() -> LatLng>
        get() = markerDataMap.values.map { { it.position } }
}

/**
 * Demonstrates how to sync a data model with a changing collection of
 * draggable markers using keys, while keeping a Polygon of the marker positions in sync with
 * the current marker position.
 *
 * The user can add a location marker to the model by clicking the map and delete a location from
 * the model by clicking a marker.
 *
 * This example builds on top of ideas from MarkersCollectionActivity and
 * SyncingDraggableMarkerWithDataModelActivity.
 */
class DraggableMarkersCollectionWithPolygonActivity : ComponentActivity() {
    // Simplistic data model from repository being set from outside (should be part of view model);
    // Only stores [LocationData], for illustration, but could hold additional data.
    private var dataModel: Map<LocationKey, LocationData> = mapOf()
        set(value) {
            field = value
            markersModel = DraggableMarkersModel(value)
        }

    private var markersModel by mutableStateOf(DraggableMarkersModel(dataModel))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MapsComposeSampleTheme {
                GoogleMapWithLocations(
                    markersModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * A GoogleMap with locations represented by markers
 */
@Composable
private fun GoogleMapWithLocations(
    markersModel: DraggableMarkersModel,
    modifier: Modifier = Modifier
) {
    val cameraPositionState = rememberCameraPositionState { position = defaultCameraPosition }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        onMapClick = { position -> markersModel.addLocation(LocationData(position)) }
    ) {
        markersModel.Markers()

        Polygon(markersModel::markerPositionsModel)
    }
}

/**
 * A draggable GoogleMap Marker representing a location on the map
 */
@Composable
private fun LocationMarker(
    markerState: MarkerState,
    onClick: () -> Unit
) {
    Marker(
        state = markerState,
        draggable = true,
        onClick = {
            onClick()

            true
        }
    )
}

/**
 * A Polygon. Helps isolate recompositions while a Marker is being dragged.
 */
@Composable
private fun Polygon(markerPositionsModel: () -> List<() -> LatLng>) {
    val movingMarkerPositions = markerPositionsModel()

    if (movingMarkerPositions.isNotEmpty()) {
        val markerPositions = movingMarkerPositions.map { it() }

        Polygon(markerPositions)
    }
}
