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

package com.google.maps.android.compose.markerexamples.markerscollection

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.defaultCameraPosition
import com.google.maps.android.compose.markerexamples.updatingnodragmarkerwithdatamodel.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.theme.MapsComposeSampleTheme

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
 * location markers using keys.
 *
 * The user can add a location marker to the model by clicking the map and delete a location from
 * the model by clicking a marker.
 *
 * This example reuses the simple non-draggable Marker approach from the
 * `UpdatingNoDragMarkerWithDataModelActivity` example, which encapsulates
 * [MarkerState] to provide a cleaner API surface.
 */
class MarkersCollectionActivity : ComponentActivity() {
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
    }
)

/**
 * A GoogleMap with locations represented by markers
 *
 * @param keyedLocationData model data for location markers with unique keys.
 * Uses a [Collection] type to keep it independent of our data model.
 * @param onAddLocation location addition events for updating data model
 * @param onDeleteLocation location deletion events for updating data model
 */
@Composable
private fun GoogleMapWithLocations(
    keyedLocationData: Collection<KeyedLocationData>,
    modifier: Modifier = Modifier,
    onAddLocation: (LocationData) -> Unit,
    onDeleteLocation: (LocationKey) -> Unit
) {
    val cameraPositionState = rememberCameraPositionState { position = defaultCameraPosition }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        onMapClick = { position -> onAddLocation(LocationData(position)) }
    ) {
        Locations(
            keyedLocationData = keyedLocationData,
            onLocationClick = onDeleteLocation
        )
    }
}

/**
 * Renders locations on a GoogleMap
 *
 * @param keyedLocationData model data for location markers with unique keys.
 * @param onLocationClick location click events
 */
@Composable
private fun Locations(
    keyedLocationData: Collection<KeyedLocationData>,
    onLocationClick: (LocationKey) -> Unit
) = keyedLocationData.forEach { (key, locationData) ->
    key(key) {
        Marker(
            position = locationData.position,
            onClick = {
                onLocationClick(key)
                true // consume click event to prevent camera move to marker
            }
        )
    }
}
