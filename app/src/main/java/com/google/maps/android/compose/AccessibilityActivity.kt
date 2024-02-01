// Copyright 2023 Google LLC
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

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.Marker

private const val TAG = "AccessibilityActivity"


class AccessibilityActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val singaporeState = rememberMarkerState(position = singapore)
            val cameraPositionState = rememberCameraPositionState {
                position = defaultCameraPosition
            }
            val uiSettings by remember { mutableStateOf(MapUiSettings(compassEnabled = false)) }
            val mapProperties by remember {
                mutableStateOf(MapProperties(mapType = MapType.NORMAL))
            }

            Box(Modifier.fillMaxSize()) {
                GoogleMap(
                    // mergeDescendants will remove accessibility from the entire map and content inside.
                    mergeDescendants = true,
                    // alternatively, contentDescription will deactivate it for the maps, but not markers.
                    contentDescription = "",
                    cameraPositionState = cameraPositionState,
                    properties = mapProperties,
                    uiSettings = uiSettings,
                    onPOIClick = {
                        Log.d(TAG, "POI clicked: ${it.name}")
                    }
                ) {
                    val markerClick: (Marker) -> Boolean = {
                        Log.d(TAG, "${it.title} was clicked")
                        cameraPositionState.projection?.let { projection ->
                            Log.d(TAG, "The current projection is: $projection")
                        }
                        false
                    }

                    Marker(
                        // contentDescription overrides title for TalkBack
                        contentDescription = "Description of the marker",
                        state = singaporeState,
                        title = "Marker in Singapore",
                        onClick = markerClick
                    )
                }
            }
        }
    }
}

