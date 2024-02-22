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

package com.google.maps.android.compose.markerexamples.markerdragevents

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.defaultCameraPosition
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.compose.singapore
import com.google.maps.android.compose.theme.MapsComposeSampleTheme
import kotlinx.coroutines.flow.dropWhile

private val TAG = MarkerDragEventsActivity::class.simpleName

/**
 * Demonstrates how to reliably generate a sequence of Marker drag START-DRAG-END events as in the
 * original GoogleMap Marker listener.
 */
class MarkerDragEventsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MapsComposeSampleTheme {
                GoogleMapWithMarker(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun GoogleMapWithMarker(
    modifier: Modifier = Modifier,
) {
    val cameraPositionState = rememberCameraPositionState { position = defaultCameraPosition }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
    ) {
        DraggableMarker(
            onDragStart = { Log.i(TAG, "onDragStart") },
            onDragEnd = { Log.i(TAG, "onDragEnd") },
            onDrag = { position -> Log.i(TAG, "onDrag: $position") }
        )
    }
}

/**
 * A draggable GoogleMap Marker.
 *
 * @param onDragStart called when marker dragging starts
 * @param onDrag called with an update for the marker's current position during dragging
 * @param onDragEnd called when marker dragging ends
 */
@Composable
private fun DraggableMarker(
    onDragStart: () -> Unit = {},
    onDrag: (LatLng) -> Unit = {},
    onDragEnd: () -> Unit = {}
) {
    val markerState = rememberMarkerState(position = singapore)

    Marker(
        state = markerState,
        draggable = true
    )

    LaunchedEffect(Unit) {
        var inDrag = false
        var priorPosition: LatLng? = singapore

        snapshotFlow { markerState.isDragging to markerState.position }
            .dropWhile { (isDragging, position) ->
                !isDragging && position == priorPosition // ignore initial value
            }
            .collect { (isDragging, position) ->
                // Do not even bother to check isDragging state here:
                // it is possible to miss a sequence of states
                // where isDragging == true, then isDragging == false;
                // in this case we would only see a change in position.
                // (Hypothetically we could even miss a change in position
                // if the Marker ended up in its original position at the
                // end of the drag. But then nothing changed at all,
                // so we should be ok to ignore this case altogether.)
                if (!inDrag) {
                    inDrag = true
                    onDragStart()
                }

                if (position != priorPosition) {
                    onDrag(position)
                    priorPosition = position
                }

                if (!isDragging) {
                    inDrag = false
                    onDragEnd()
                }
            }
    }
}
