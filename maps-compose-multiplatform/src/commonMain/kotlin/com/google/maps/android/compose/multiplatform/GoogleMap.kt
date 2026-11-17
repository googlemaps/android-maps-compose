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

package com.google.maps.android.compose.multiplatform

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.maps.android.model.LatLng

/**
 * A Google Map, on Android through maps-compose and on iOS through the Google Maps SDK for iOS.
 *
 * The API follows maps-compose, so code written against it ports between the two with few
 * changes. Draw on the map with [Marker], [Polyline], [Polygon], [Circle] and [Clustering] in
 * [content].
 *
 * On iOS, the app must call `GMSServices.provideAPIKey` before showing a map.
 *
 * @param cameraPositionState controls and observes the camera
 * @param properties the map's content properties
 * @param uiSettings the map's controls and gestures
 * @param onMapClick called with the tapped location, when the tap hits no marker or shape
 * @param onMapLongClick called with the long-pressed location
 * @param onMapLoaded called once the first tiles have rendered
 * @param content the markers, shapes and clusters to draw on the map
 */
@Composable
public fun GoogleMap(
    modifier: Modifier = Modifier,
    cameraPositionState: CameraPositionState = rememberCameraPositionState(),
    properties: MapProperties = MapProperties(),
    uiSettings: MapUiSettings = MapUiSettings(),
    onMapClick: (LatLng) -> Unit = {},
    onMapLongClick: (LatLng) -> Unit = {},
    onMapLoaded: () -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    val registry = remember { MapNodeRegistry() }
    Box(modifier) {
        PlatformGoogleMap(
            modifier = Modifier.matchParentSize(),
            cameraPositionState = cameraPositionState,
            properties = properties,
            uiSettings = uiSettings,
            onMapClick = onMapClick,
            onMapLongClick = onMapLongClick,
            onMapLoaded = onMapLoaded,
            registry = registry,
        )
        // Map content emits no layout: it only registers nodes, which the platform map draws.
        CompositionLocalProvider(
            LocalMapNodeRegistry provides registry,
            LocalCameraPositionState provides cameraPositionState,
        ) {
            content()
        }
    }
}

/** Shows the platform map and draws the nodes in [registry] on it. */
@Composable
internal expect fun PlatformGoogleMap(
    modifier: Modifier,
    cameraPositionState: CameraPositionState,
    properties: MapProperties,
    uiSettings: MapUiSettings,
    onMapClick: (LatLng) -> Unit,
    onMapLongClick: (LatLng) -> Unit,
    onMapLoaded: () -> Unit,
    registry: MapNodeRegistry,
)
