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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import cocoapods.GoogleMaps.*

import platform.UIKit.UIViewController
import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import platform.CoreLocation.CLLocationCoordinate2DMake

@OptIn(ExperimentalForeignApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
public actual fun GoogleMap(
    modifier: Modifier,
    latitude: Double,
    longitude: Double,
    zoom: Float,
    mapType: MapType,
    myLocationEnabled: Boolean,
    scrollGesturesEnabled: Boolean,
    zoomGesturesEnabled: Boolean,
    markers: List<MapMarker>
) {
    UIKitView(
        factory = {
            val camera = GMSCameraPosition.cameraWithLatitude(latitude, longitude, zoom)
            GMSMapView.mapWithFrame(CGRectZero.readValue(), camera = camera)
        },
        modifier = modifier,
        properties = UIKitInteropProperties(
            interactionMode = UIKitInteropInteractionMode.NonCooperative
        ),
        update = { mapView ->
            val camera = GMSCameraPosition.cameraWithLatitude(latitude, longitude, zoom)
            mapView.animateToCameraPosition(camera)

            // Update map type
            mapView.mapType = when (mapType) {
                MapType.NONE -> kGMSTypeNone
                MapType.NORMAL -> kGMSTypeNormal
                MapType.SATELLITE -> kGMSTypeSatellite
                MapType.TERRAIN -> kGMSTypeTerrain
                MapType.HYBRID -> kGMSTypeHybrid
            }

            // Update my location
            mapView.myLocationEnabled = myLocationEnabled
            mapView.settings.myLocationButton = myLocationEnabled

            // Update gestures
            mapView.settings.scrollGestures = scrollGesturesEnabled
            mapView.settings.zoomGestures = zoomGesturesEnabled

            // Clear old and add new markers
            mapView.clear()
            markers.forEach { markerData ->
                val marker = GMSMarker()
                marker.position = CLLocationCoordinate2DMake(markerData.latitude, markerData.longitude)
                marker.title = markerData.title
                marker.snippet = markerData.snippet
                marker.map = mapView
            }
        }
    )
}

public fun GoogleMapViewController(
    latitude: Double,
    longitude: Double,
    zoom: Float
): UIViewController {
    return GoogleMapViewController(
        latitude = latitude,
        longitude = longitude,
        zoom = zoom,
        mapType = MapType.NORMAL,
        myLocationEnabled = false,
        scrollGesturesEnabled = true,
        zoomGesturesEnabled = true,
        markers = emptyList()
    )
}

public fun GoogleMapViewController(
    latitude: Double,
    longitude: Double,
    zoom: Float,
    markerLatitude: Double?,
    markerLongitude: Double?,
    markerTitle: String?
): UIViewController {
    val markersList = if (markerLatitude != null && markerLongitude != null) {
        listOf(MapMarker(latitude = markerLatitude, longitude = markerLongitude, title = markerTitle))
    } else {
        emptyList()
    }
    return GoogleMapViewController(
        latitude = latitude,
        longitude = longitude,
        zoom = zoom,
        mapType = MapType.NORMAL,
        myLocationEnabled = false,
        scrollGesturesEnabled = true,
        zoomGesturesEnabled = true,
        markers = markersList
    )
}

public fun GoogleMapViewController(
    latitude: Double,
    longitude: Double,
    zoom: Float,
    mapType: MapType,
    myLocationEnabled: Boolean,
    scrollGesturesEnabled: Boolean,
    zoomGesturesEnabled: Boolean,
    markers: List<MapMarker>
): UIViewController {
    return ComposeUIViewController(configure = {
        enforceStrictPlistSanityCheck = false
    }) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            latitude = latitude,
            longitude = longitude,
            zoom = zoom,
            mapType = mapType,
            myLocationEnabled = myLocationEnabled,
            scrollGesturesEnabled = scrollGesturesEnabled,
            zoomGesturesEnabled = zoomGesturesEnabled,
            markers = markers
        )
    }
}

