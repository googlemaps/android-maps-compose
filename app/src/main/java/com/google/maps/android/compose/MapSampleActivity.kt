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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.VectorApplier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.LatLng

class MapSampleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapsInitializer.initialize(this)
        setContent {
            val cameraPositionState = rememberCameraPositionState()
            val mapProperties = rememberMapPropertiesState()
            var shouldAnimateZoom by remember { mutableStateOf(true) }
            var ticker by remember { mutableStateOf(0) }

            Box(Modifier.fillMaxSize()) {
                GoogleMap(
                    modifier = Modifier.matchParentSize(),
                    mapProperties = mapProperties,
                    cameraPositionState = cameraPositionState,
                ) {
                    Marker(
                        position = LatLng(-34.0, 151.0),
                        title = "Zoom in has been tapped $ticker times.",
                        onClick = {
                            println("Marker was clicked: $it")
                            false
                        }
                    )
                    Circle(
                        center = LatLng(-34.1, 151.0),
                        radius = 100.0,
                    )
                }

                Column {
                    MapTypeControls(onMapTypeClick = {
                        mapProperties.mapType = it
                    })
                    ZoomControls(
                        shouldAnimateZoom,
                        onZoomOut = {
                            cameraPositionState.zoomOut(shouldAnimateZoom)
                            ticker--
                        },
                        onZoomIn = {
                            cameraPositionState.zoomIn(shouldAnimateZoom)
                            ticker++
                        },
                        onCheckChange = {
                            shouldAnimateZoom = it
                        }
                    )
                    DebugView(cameraPositionState)
                }
            }
        }
    }
}

@Composable
private fun MapTypeControls(
    onMapTypeClick: (MapType) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(state = ScrollState(0)),
        horizontalArrangement = Arrangement.Center
    ) {
        MapType.values().forEach {
            MapTypeButton(type = it) { onMapTypeClick(it) }
        }
    }
}

@Composable
private fun MapTypeButton(type: MapType, onClick: () -> Unit) {
    Button(
        modifier = Modifier.padding(4.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.onPrimary,
            contentColor = MaterialTheme.colors.primary
        ),
        onClick = onClick
    ) {
        Text(text = type.toString(), style = MaterialTheme.typography.body1)
    }
}

@Composable
private fun ZoomControls(
    isChecked: Boolean,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    onCheckChange: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        MapButton("-", onClick = { onZoomOut() })
        MapButton("+", onClick = { onZoomIn() })
        Switch(isChecked, onCheckedChange = onCheckChange)
    }
}

@Composable
private fun MapButton(text: String, onClick: () -> Unit) {
    Button(
        modifier = Modifier.padding(8.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.onPrimary,
            contentColor = MaterialTheme.colors.primary
        ),
        onClick = onClick
    ) {
        Text(text = text, style = MaterialTheme.typography.h5)
    }
}

@Composable
private fun DebugView(cameraPositionState: CameraPositionState) {
    Column(
        Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Camera state is ${cameraPositionState.cameraState}")
        Text(text = "Camera position is ${cameraPositionState.cameraPosition}")
    }
}