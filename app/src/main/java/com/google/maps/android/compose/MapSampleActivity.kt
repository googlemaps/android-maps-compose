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
import android.util.Log
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import kotlinx.coroutines.launch

class MapSampleActivity : ComponentActivity() {

    private val TAG = MapSampleActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapsInitializer.initialize(this)
        setContent {
            val sanFrancisco = LatLng(37.76, -122.47)

            // Observing and controlling the camera's state can be done with a CameraPositionState
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(sanFrancisco, 10f)
            }

            var mapType by remember { mutableStateOf(MapType.NORMAL) }
            var uiSettings by remember { mutableStateOf(MapUiSettings(compassEnabled = false)) }
            var shouldAnimateZoom by remember { mutableStateOf(true) }
            var ticker by remember { mutableStateOf(0) }

            Box(Modifier.fillMaxSize()) {
                GoogleMap(
                    modifier = Modifier.matchParentSize(),
                    mapType = mapType,
                    cameraPositionState = cameraPositionState,
                    uiSettings = uiSettings,
                    mapEventListener = object : MapEventListener {
                        override fun onPOIClick(poi: PointOfInterest) {
                            Log.d(TAG, "$poi was clicked")
                        }
                    }
                ) {
                    // Drawing on the map is accomplished with a child-based API
                    Marker(
                        position = sanFrancisco,
                        title = "Zoom in has been tapped $ticker times.",
                        onClick = {
                            Log.d(TAG, "${it.title} was clicked")
                            false
                        }
                    )
                    Circle(
                        center = sanFrancisco,
                        fillColor = MaterialTheme.colors.secondary,
                        strokeColor = MaterialTheme.colors.secondaryVariant,
                        radius = 1000.0,
                    )
                }

                Column {
                    MapTypeControls(onMapTypeClick = {
                        Log.d("GoogleMap", "Selected map type $it")
                        mapType = it
                    })
                    val coroutineScope = rememberCoroutineScope()
                    ZoomControls(
                        shouldAnimateZoom,
                        uiSettings.zoomControlsEnabled,
                        onZoomOut = {
                            if (shouldAnimateZoom) {
                                coroutineScope.launch {
                                    cameraPositionState.animate(CameraUpdateFactory.zoomOut())
                                }
                            } else {
                                cameraPositionState.move(CameraUpdateFactory.zoomOut())
                            }
                        },
                        onZoomIn = {
                            if (shouldAnimateZoom) {
                                coroutineScope.launch {
                                    cameraPositionState.animate(CameraUpdateFactory.zoomIn())
                                }
                            } else {
                                cameraPositionState.move(CameraUpdateFactory.zoomIn())
                            }
                            ticker++
                        },
                        onCameraAnimationCheckedChange = {
                            shouldAnimateZoom = it
                        },
                        onZoomControlsCheckedChange = {
                            uiSettings = uiSettings.copy(zoomControlsEnabled = it)
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
    isCameraAnimationChecked: Boolean,
    isZoomControlsEnabledChecked: Boolean,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    onCameraAnimationCheckedChange: (Boolean) -> Unit,
    onZoomControlsCheckedChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        MapButton("-", onClick = { onZoomOut() })
        MapButton("+", onClick = { onZoomIn() })
        Column(verticalArrangement = Arrangement.Center) {
            Row(horizontalArrangement = Arrangement.Center) {
                Text(text = "Camera Animations On?")
                Switch(isCameraAnimationChecked, onCheckedChange = onCameraAnimationCheckedChange)
            }
            Row(horizontalArrangement = Arrangement.Center) {
                Text(text = "Zoom Controls On?")
                Switch(isZoomControlsEnabledChecked, onCheckedChange = onZoomControlsCheckedChange)
            }
        }
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
        val moving = if (cameraPositionState.isMoving) "moving" else "not moving"
        Text(text = "Camera is $moving")
        Text(text = "Camera position is ${cameraPositionState.position}")
    }
}