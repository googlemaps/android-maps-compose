/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.maps.android.compose.multiplatform.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.compose.multiplatform.Circle
import com.google.maps.android.compose.multiplatform.Clustering
import com.google.maps.android.compose.multiplatform.GoogleMap
import com.google.maps.android.compose.multiplatform.MapProperties
import com.google.maps.android.compose.multiplatform.MapType
import com.google.maps.android.compose.multiplatform.MapUiSettings
import com.google.maps.android.compose.multiplatform.Marker
import com.google.maps.android.compose.multiplatform.Polygon
import com.google.maps.android.compose.multiplatform.Polyline
import com.google.maps.android.compose.multiplatform.cameraPosition
import com.google.maps.android.compose.multiplatform.rememberCameraPositionState
import com.google.maps.android.compose.multiplatform.rememberMarkerState
import com.google.maps.android.model.LatLng
import com.google.maps.android.model.latitude
import com.google.maps.android.model.longitude
import com.google.maps.android.model.target
import com.google.maps.android.model.zoom
import kotlin.math.round
import kotlin.random.Random
import kotlinx.coroutines.launch

private val SanFrancisco = LatLng(37.7749, -122.4194)
private val London = LatLng(51.5072, -0.1276)
private val Sydney = LatLng(-33.8688, 151.2093)

@Composable
internal fun CameraDemo() {
    val camera = rememberCameraPositionState { position = cameraPosition(SanFrancisco, 11f) }
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxSize()) {
        GoogleMap(Modifier.fillMaxSize(), cameraPositionState = camera)
        DemoControls(Modifier.align(Alignment.TopStart)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("San Francisco" to SanFrancisco, "London" to London, "Sydney" to Sydney)
                    .forEach { (name, target) ->
                        DemoButton(name) {
                            scope.launch { camera.animate(cameraPosition(target, 11f)) }
                        }
                    }
            }
            Spacer(Modifier.height(8.dp))
            val target = camera.position.target
            DemoLabel(
                "${target.latitude.format()}, ${target.longitude.format()} at zoom " +
                    camera.position.zoom.toDouble().format() +
                    if (camera.isMoving) " (moving)" else "",
            )
        }
    }
}

@Composable
internal fun MarkersDemo() {
    val camera = rememberCameraPositionState { position = cameraPosition(SanFrancisco, 12f) }
    val dragged = rememberMarkerState(LatLng(37.7599, -122.4148))
    var taps by remember { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize()) {
        GoogleMap(Modifier.fillMaxSize(), cameraPositionState = camera) {
            Marker(
                state = rememberMarkerState(LatLng(37.8199, -122.4783)),
                title = "Golden Gate Bridge",
                snippet = "Tap shows this info window",
                onClick = { taps++; false },
            )
            Marker(
                state = rememberMarkerState(LatLng(37.8024, -122.4058)),
                title = "Coit Tower",
                onClick = { taps++; false },
            )
            Marker(
                state = dragged,
                title = "Drag me",
                snippet = "Long press, then drag",
                draggable = true,
            )
        }
        DemoControls(Modifier.align(Alignment.TopStart)) {
            DemoLabel(
                "Marker taps: $taps\nDragged to ${dragged.position.latitude.format()}, " +
                    dragged.position.longitude.format(),
            )
        }
    }
}

@Composable
internal fun ShapesDemo() {
    val camera = rememberCameraPositionState { position = cameraPosition(SanFrancisco, 11f) }
    var lastTap by remember { mutableStateOf("Tap a shape") }
    Box(Modifier.fillMaxSize()) {
        GoogleMap(Modifier.fillMaxSize(), cameraPositionState = camera) {
            Polyline(
                points = listOf(
                    LatLng(37.8199, -122.4783),
                    LatLng(37.8024, -122.4058),
                    LatLng(37.7955, -122.3937),
                ),
                color = Accent,
                width = 6f,
                clickable = true,
                onClick = { lastTap = "Polyline" },
            )
            Polygon(
                points = listOf(
                    LatLng(37.7700, -122.5100),
                    LatLng(37.7700, -122.4500),
                    LatLng(37.7400, -122.4500),
                    LatLng(37.7400, -122.5100),
                ),
                holes = listOf(
                    listOf(
                        LatLng(37.7600, -122.4900),
                        LatLng(37.7600, -122.4700),
                        LatLng(37.7500, -122.4700),
                        LatLng(37.7500, -122.4900),
                    ),
                ),
                fillColor = Color(0x5534A853),
                strokeColor = Color(0xFF34A853),
                clickable = true,
                onClick = { lastTap = "Polygon" },
            )
            Circle(
                center = LatLng(37.7599, -122.4148),
                radius = 1_500.0,
                fillColor = Color(0x55EA4335),
                strokeColor = Color(0xFFEA4335),
                clickable = true,
                onClick = { lastTap = "Circle" },
            )
        }
        DemoControls(Modifier.align(Alignment.TopStart)) {
            DemoLabel("Last tap: $lastTap")
        }
    }
}

private class Place(override val position: LatLng, override val title: String) : ClusterItem {
    override val snippet: String? = null
    override val zIndex: Float? = null
}

@Composable
internal fun ClusteringDemo() {
    val camera = rememberCameraPositionState { position = cameraPosition(London, 9f) }
    // A fixed seed, so the demo looks the same every time and on both platforms.
    val places = remember {
        val random = Random(42)
        List(300) { index ->
            Place(
                LatLng(
                    London.latitude + (random.nextDouble() - 0.5) * 0.6,
                    London.longitude + (random.nextDouble() - 0.5) * 0.9,
                ),
                "Place ${index + 1}",
            )
        }
    }
    Box(Modifier.fillMaxSize()) {
        GoogleMap(Modifier.fillMaxSize(), cameraPositionState = camera) {
            Clustering(places)
        }
        DemoControls(Modifier.align(Alignment.TopStart)) {
            DemoLabel("Zoom ${camera.position.zoom.toDouble().format()}: tap a cluster to zoom in")
        }
    }
}

@Composable
internal fun MapSettingsDemo() {
    var properties by remember { mutableStateOf(MapProperties()) }
    var uiSettings by remember { mutableStateOf(MapUiSettings()) }
    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            Modifier.fillMaxSize(),
            cameraPositionState = rememberCameraPositionState {
                position = cameraPosition(LatLng(40.7580, -73.9855), 15f)
            },
            properties = properties,
            uiSettings = uiSettings,
        )
        DemoControls(Modifier.align(Alignment.TopStart)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DemoButton("Type: ${properties.mapType.name.lowercase()}") {
                    val types = MapType.entries
                    properties = properties.copy(
                        mapType = types[(properties.mapType.ordinal + 1) % types.size],
                    )
                }
                DemoButton("Traffic: ${properties.isTrafficEnabled.onOff()}") {
                    properties = properties.copy(isTrafficEnabled = !properties.isTrafficEnabled)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DemoButton("Buildings: ${properties.isBuildingEnabled.onOff()}") {
                    properties = properties.copy(isBuildingEnabled = !properties.isBuildingEnabled)
                }
                DemoButton("Gestures: ${uiSettings.scrollGesturesEnabled.onOff()}") {
                    val enabled = !uiSettings.scrollGesturesEnabled
                    uiSettings = uiSettings.copy(
                        scrollGesturesEnabled = enabled,
                        zoomGesturesEnabled = enabled,
                        rotationGesturesEnabled = enabled,
                        tiltGesturesEnabled = enabled,
                    )
                }
            }
        }
    }
}

@Composable
internal fun MapClicksDemo() {
    val dropped = remember { mutableStateListOf<LatLng>() }
    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            Modifier.fillMaxSize(),
            cameraPositionState = rememberCameraPositionState {
                position = cameraPosition(Sydney, 12f)
            },
            onMapClick = { dropped += it },
            onMapLongClick = { dropped.clear() },
        ) {
            dropped.forEachIndexed { index, position ->
                Marker(state = rememberMarkerState(position), title = "Marker ${index + 1}")
            }
        }
        DemoControls(Modifier.align(Alignment.TopStart)) {
            DemoLabel("${dropped.size} markers: tap to add, long press to clear")
        }
    }
}

private fun Double.format(): String = (round(this * 10_000) / 10_000).toString()

private fun Boolean.onOff(): String = if (this) "on" else "off"
