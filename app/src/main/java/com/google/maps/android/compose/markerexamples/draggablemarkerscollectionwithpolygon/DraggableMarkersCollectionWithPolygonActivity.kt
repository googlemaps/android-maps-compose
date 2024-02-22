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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.defaultCameraPosition
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.theme.MapsComposeSampleTheme
import kotlinx.coroutines.flow.drop

/**
 * Simplistic app data model intended for persistent storage.
 *
 * This only stores [LocationData], for demonstration purposes, but could hold an entire app's data.
 */
private class DataModel {
    /**
     * Location data.
     */
    val locationDataMap = mutableStateMapOf<LocationKey, LocationData>()
}

/**
 * Data type representing a location.
 *
 * This only stores location position, for demonstration purposes,
 * but could hold other data related to the location.
 */
@Immutable
private data class LocationData(val position: LatLng)

/**
 * Unique, stable key for location
 */
private class LocationKey

private typealias KeyedLocationData = Pair<LocationKey, LocationData>

/**
 * Demonstrates how to sync a data model with a changing collection of
 * draggable markers using keys, while keeping a Polygon of the marker positions in sync with
 * the current marker position. When dragging: the data model is updated only once dragging has
 * ended (user released marker), as a model update might trigger other costly operations.
 *
 * The user can add a location marker to the model by clicking the map and delete a location from
 * the model by clicking a marker.
 *
 * This example builds on top of ideas from MarkersCollectionActivity and
 * SyncingDraggableMarkerWithDataModelActivity.
 */
class DraggableMarkersCollectionWithPolygonActivity : ComponentActivity() {
    private val dataModel = DataModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MapsComposeSampleTheme {
                Screen(
                    dataModel = dataModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun Screen(
    dataModel: DataModel,
    modifier: Modifier = Modifier
) = GoogleMapWithLocations(
    modifier = modifier,
    keyedLocationData = dataModel.locationDataMap.toList(),
    onAddLocation = { locationData ->
        dataModel.locationDataMap += LocationKey() to locationData
    },
    onDeleteLocation = { key ->
        dataModel.locationDataMap -= key
    },
    onMoveLocation = { key, locationData ->
        dataModel.locationDataMap[key] = locationData
    }
)

/**
 * A GoogleMap with locations represented by markers
 */
@Composable
private fun GoogleMapWithLocations(
    keyedLocationData: Collection<KeyedLocationData>,
    modifier: Modifier = Modifier,
    onAddLocation: (LocationData) -> Unit,
    onDeleteLocation: (LocationKey) -> Unit,
    onMoveLocation: (LocationKey, LocationData) -> Unit
) {
    val cameraPositionState = rememberCameraPositionState { position = defaultCameraPosition }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        onMapClick = { position -> onAddLocation(LocationData(position)) }
    ) {
        Locations(
            keyedLocationData = keyedLocationData,
            onLocationClick = onDeleteLocation,
            onLocationUpdate = onMoveLocation
        )
    }
}

/**
 * Renders locations on a GoogleMap
 */
@Composable
private fun Locations(
    keyedLocationData: Collection<KeyedLocationData>,
    onLocationClick: (LocationKey) -> Unit,
    onLocationUpdate: (LocationKey, LocationData) -> Unit
) {
    // This doubles as a handy trick to leverage Compose's node matching algorithm for
    // generating a list of marker positions derived from the original model
    val movingVertices: List<() -> LatLng> = keyedLocationData.map { (key, locationData) ->
        key(key) {
            // This sets the MarkerData from our model once (model is initial source of truth)
            // and never updates it from the model afterwards.
            // See SyncingDraggableMarkerWithDataModelActivity for rationale.
            val markerState = remember { MarkerState(locationData.position) }

            LocationMarker(
                markerState,
                onClick = { onLocationClick(key) },
                onDragEnd = {
                    val newLocationData = LocationData(markerState.position)
                    onLocationUpdate(key, newLocationData)
                }
            )

            markerState::position // share only read access to MarkerState.position
        }
    }

    Polygon(movingVertices)
}

/**
 * A draggable GoogleMap Marker representing a location on the map
 */
@Composable
private fun LocationMarker(
    markerState: MarkerState,
    onClick: () -> Unit,
    onDragEnd: () -> Unit
) {
    Marker(
        state = markerState,
        draggable = true,
        onClick = {
            onClick()

            true
        }
    )

    LaunchedEffect(Unit) {
        snapshotFlow { markerState.isDragging }
            .drop(1) // ignore initial value
            .collect { isDragging ->
                if (!isDragging) onDragEnd()
            }
    }
}

/**
 * A Polygon. Helps isolate recompositions while a Marker is being dragged.
 */
@Composable
private fun Polygon(movingVertices: List<() -> LatLng>) {
    if (movingVertices.isNotEmpty()) {
        val markerPositions = movingVertices.map { it() }

        Polygon(markerPositions)
    }
}
