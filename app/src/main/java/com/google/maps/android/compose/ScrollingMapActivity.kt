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
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker

private const val TAG = "ScrollingMapActivity"

private val singapore = LatLng(1.35, 103.87)
private val defaultCameraPosition = CameraPosition.fromLatLngZoom(singapore, 11f)

class ScrollingMapActivity : ComponentActivity() {

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Observing and controlling the camera's state can be done with a CameraPositionState
            val cameraPositionState = rememberCameraPositionState {
                position = defaultCameraPosition
            }
            var scrollingEnabled by remember { mutableStateOf(true) }

            // Use a LaunchedEffect keyed on the camera moving state to enable scrolling when the camera stops moving
            LaunchedEffect(cameraPositionState.isMoving) {
                if (!cameraPositionState.isMoving) {
                    scrollingEnabled = true
                    Log.d(TAG, "Map camera stopped moving - Enabling column scrolling...")
                }
            }

            ColumnWithMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState,
                scrollingEnabled = scrollingEnabled,
                onMapTouched = {
                    scrollingEnabled = false
                    Log.d(
                        TAG,
                        "User touched map - Disabling column scrolling after user touched this Box..."
                    )
                },
                onMapLoaded = { }
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ColumnWithMap(
    modifier: Modifier = Modifier,
    cameraPositionState: CameraPositionState,
    scrollingEnabled: Boolean,
    onMapTouched: () -> Unit,
    onMapLoaded: () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colors.background
    ) {
        var isMapLoaded by remember { mutableStateOf(false) }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState(),
                    scrollingEnabled
                ),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.padding(10.dp))
            for (i in 1..20) {
                Text(
                    text = "Item $i",
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .testTag("Item $i")
                )
            }
            Spacer(modifier = Modifier.padding(10.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                GoogleMapViewInColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    onMapLoaded = {
                        isMapLoaded = true
                        onMapLoaded()
                    },
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInteropFilter(
                            onTouchEvent = {
                                when (it.action) {
                                    MotionEvent.ACTION_DOWN -> {
                                        onMapTouched()
                                        false
                                    }
                                    else -> {
                                        Log.d(
                                            TAG,
                                            "MotionEvent ${it.action} - this never triggers."
                                        )
                                        true
                                    }
                                }
                            }
                        )
                )
                if (!isMapLoaded) {
                    androidx.compose.animation.AnimatedVisibility(
                        modifier = Modifier
                            .fillMaxSize(),
                        visible = !isMapLoaded,
                        enter = EnterTransition.None,
                        exit = fadeOut()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .background(MaterialTheme.colors.background)
                                .wrapContentSize()
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.padding(10.dp))
            for (i in 21..40) {
                Text(
                    text = "Item $i",
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .testTag("Item $i")
                )
            }
            Spacer(modifier = Modifier.padding(10.dp))
        }
    }
}

@Composable
private fun GoogleMapViewInColumn(
    modifier: Modifier,
    cameraPositionState: CameraPositionState,
    onMapLoaded: () -> Unit,
) {
    val singaporeState = rememberMarkerState(position = singapore)

    var uiSettings by remember { mutableStateOf(MapUiSettings(compassEnabled = false)) }
    var mapProperties by remember {
        mutableStateOf(MapProperties(mapType = MapType.NORMAL))
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = uiSettings,
        onMapLoaded = onMapLoaded
    ) {
        // Drawing on the map is accomplished with a child-based API
        val markerClick: (Marker) -> Boolean = {
            Log.d(TAG, "${it.title} was clicked")
            cameraPositionState.projection?.let { projection ->
                Log.d(TAG, "The current projection is: $projection")
            }
            false
        }
        MarkerInfoWindowContent(
            state = singaporeState,
            title = "Singapore",
            onClick = markerClick,
            draggable = true,
        ) {
            Text(it.title ?: "Title", color = Color.Red)
        }
    }
}