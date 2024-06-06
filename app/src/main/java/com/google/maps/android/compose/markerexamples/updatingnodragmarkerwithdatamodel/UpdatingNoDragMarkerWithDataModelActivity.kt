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

package com.google.maps.android.compose.markerexamples.updatingnodragmarkerwithdatamodel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.defaultCameraPosition
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.compose.singapore
import com.google.maps.android.compose.singapore2
import com.google.maps.android.compose.singapore3
import com.google.maps.android.compose.theme.MapsComposeSampleTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

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
 * Demonstrates how to easily initialize and update position for a non-draggable
 * Marker from a data model.
 */
class UpdatingNoDragMarkerWithDataModelActivity : ComponentActivity() {
    private val dataModel = DataModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            // Simulate remote updates to data model
            while (true) {
                delay(3_000)

                val newPosition = when (Random.nextInt(3)) {
                    0 -> singapore
                    1 -> singapore2
                    2 -> singapore3
                    else -> singapore
                }

                dataModel.locationData = LocationData(newPosition)
            }
        }

        setContent {
            MapsComposeSampleTheme {
                GoogleMapWithSimpleMarker(
                    locationData = dataModel.locationData,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun GoogleMapWithSimpleMarker(
    locationData: LocationData,
    modifier: Modifier = Modifier,
) {
    val cameraPositionState = rememberCameraPositionState { position = defaultCameraPosition }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
    ) {
        Marker(position = locationData.position)
    }
}

/**
 * Standard API pattern for a non-draggable Marker.
 *
 * The caller does not have to deal with MarkerState,
 * and can update Marker [position] via recomposition.
 */
@Composable
fun Marker(
    position: LatLng,
    onClick: () -> Boolean = { false },
) {
    val markerState = rememberUpdatedMarkerState(position)

    Marker(
        state = markerState,
        onClick = { onClick() }
    )
}

/**
 * Standard API pattern for remembering, initializing, and updating MarkerState for a
 * non-draggable Marker, where [position] comes from a data model.
 *
 * Implementation modeled after `rememberUpdatedState`.
 *
 * This one uses [remember] behind the scenes, not [rememberMarkerState], which uses
 * `rememberSaveable`. Our data model is the source of truth - `rememberSaveable` would
 * create a conflicting source of truth.
 */
@Composable
fun rememberUpdatedMarkerState(position: LatLng): MarkerState =
    // This pattern is equivalent to what rememberUpdatedState() does:
    // rememberUpdatedState() uses MutableState, we use MarkerState.
    // This is more efficient than updating position in an effect,
    // as we avoid an additional recomposition.
    remember { MarkerState(position = position) }.also {
        it.position = position
    }
