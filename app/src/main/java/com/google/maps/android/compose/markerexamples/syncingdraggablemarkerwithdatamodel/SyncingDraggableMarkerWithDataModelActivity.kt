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

package com.google.maps.android.compose.markerexamples.syncingdraggablemarkerwithdatamodel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.defaultCameraPosition
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.singapore
import com.google.maps.android.compose.theme.MapsComposeSampleTheme

/**
 * Simplistic app data model intended for persistent storage.
 *
 * This only stores [LocationData], for demonstration purposes, but could hold an entire app's data.
 */
private class DataModel {
    /**
     * Location data
     */
    var locationData by mutableStateOf(LocationData(singapore))
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
 * Demonstrates how to avoid data races when keeping a data model in sync
 * with location derived from a draggable marker. The model is the initial source of truth for the
 * marker's position; markers are draggable, so MarkerState becomes the source of truth after
 * initialization.
 *
 * This addresses difficulties caused by having source of truth for position baked into
 * com.google.android.gms.maps.model.Marker, and consequently MarkerState.
 */
class SyncingDraggableMarkerWithDataModelActivity : ComponentActivity() {
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
) {
    GoogleMapWithLocation(
        modifier = modifier,
        locationData = dataModel.locationData,
        onUpdateLocation = { locationData ->
            dataModel.locationData = locationData
        }
    )
}

/**
 * A GoogleMap with a location represented by a marker
 *
 * @param locationData model data for location marker. The UI becomes the source of truth for
 * marker position after initial composition; the model's position is ignored on recomposition.
 * @param onUpdateLocation location update events for updating data model
 */
@Composable
private fun GoogleMapWithLocation(
    locationData: LocationData,
    modifier: Modifier = Modifier,
    onUpdateLocation: (LocationData) -> Unit
) {
    val cameraPositionState = rememberCameraPositionState { position = defaultCameraPosition }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
    ) {
        LocationMarker(
            locationData = locationData,
            onLocationUpdate = onUpdateLocation
        )
    }
}

/**
 * A draggable GoogleMap Marker representing a location on the map.
 *
 * @param locationData model data for location marker. The UI becomes the source of truth for
 * marker position after initial composition; the model's position is ignored on recomposition.
 * @param onLocationUpdate marker update events with updated [LocationData]
 */
@Composable
private fun LocationMarker(
    locationData: LocationData,
    onLocationUpdate: (LocationData) -> Unit
) {
    // This sets the MarkerData from our model once (model is initial source of truth)
    // and never updates it from the model afterwards,
    // because MarkerState/GoogleMap is the source of truth after initialization
    // and we want to avoid multiple competing sources of truth
    // to prevent potential data races.
    // This achieves a clean separation of sources of truth at the cost of
    // no longer having state flow down.
    // It is the price we pay for having source of truth baked into
    // com.google.android.gms.maps.model.Marker, and consequently MarkerState.
    //
    // Do not use rememberMarkerState() here, because it uses rememberSaveable();
    // we want to save the position to persistent storage as part of our data model
    // instead - rememberSaveable() would add a conflicting source of truth.
    val markerState = remember { MarkerState(locationData.position) }

    Marker(
        state = markerState,
        draggable = true
    )

    LaunchedEffect(Unit) {
        snapshotFlow { markerState.position }
            .collect { position ->
                // build LocationData update from marker update
                val update = LocationData(position = position)

                // send update event
                onLocationUpdate(update)
            }
    }
}
