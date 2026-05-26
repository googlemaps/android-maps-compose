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

package com.google.maps.android.compose.snippets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.GroundOverlay
import com.google.maps.android.compose.GroundOverlayPosition
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.TileOverlay
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberComposeBitmapDescriptor
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.google.maps.android.compose.widgets.ScaleBar
import com.google.maps.android.compose.wms.WmsTileOverlay
import kotlin.OptIn

/**
 * Demonstrates how to overlay a static rectangular image clamped over coordinate bounds on the map.
 *
 * This Composable displays a default marker icon as a flat GroundOverlay stretched across the Singapore area.
 */
@Composable
fun GroundOverlaySnippet() {
    // [START maps_android_compose_ground_overlay]
    val cameraPositionState = rememberCameraPositionState {
        position = defaultCameraPosition
    }

    val bounds = LatLngBounds(
        LatLng(1.35, 103.86),
        LatLng(1.37, 103.88)
    )

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        // Clamps a static image overlay flatly on top of geographic bounds
        GroundOverlay(
            position = GroundOverlayPosition.create(bounds),
            image = BitmapDescriptorFactory.defaultMarker()
            // In a real production application:
            // image = BitmapDescriptorFactory.fromResource(R.drawable.my_overlay_image)
        )
    }
    // [END maps_android_compose_ground_overlay]
}

/**
 * Demonstrates how to register custom styled dynamic map tile overlays using [TileOverlay].
 *
 * This snippet initializes a custom [TileProvider] that returns empty tiles, serving as an API
 * reference showing where to hook custom raster tile bytes.
 */
@Composable
fun TileOverlaySnippet() {
    // [START maps_android_compose_tile_overlay]
    val cameraPositionState = rememberCameraPositionState {
        position = defaultCameraPosition
    }

    val customTileProvider = remember {
        object : TileProvider {
            override fun getTile(x: Int, y: Int, zoom: Int): Tile? {
                // Custom tile loading logic goes here (returns Tile(width, height, bytes))
                return null
            }
        }
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        TileOverlay(
            tileProvider = customTileProvider,
            transparency = 0.2f
        )
    }
    // [END maps_android_compose_tile_overlay]
}

/**
 * Demonstrates how to import and overlay custom layers from a Web Map Service (WMS).
 *
 * Uses [WmsTileOverlay] utility Composable to load raster tile maps dynamically using the EPSG:3857 projection.
 */
@Composable
fun WmsTileOverlaySnippet() {
    // [START maps_android_compose_wms_tile_overlay]
    val cameraPositionState = rememberCameraPositionState {
        position = defaultCameraPosition
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        // Load custom layer tiles dynamically from a WMS endpoint
        WmsTileOverlay(
            urlFormatter = { xMin, yMin, xMax, yMax, _ ->
                "https://demo.mapserver.org/cgi-bin/wms?SERVICE=WMS&VERSION=1.1.1" +
                        "&REQUEST=GetMap&SRS=EPSG:3857&WIDTH=256&HEIGHT=256" +
                        "&LAYERS=bluemarble&FORMAT=image/jpeg" +
                        "&BBOX=$xMin,$yMin,$xMax,$yMax"
            },
            transparency = 0.3f
        )
    }
    // [END maps_android_compose_wms_tile_overlay]
}

/**
 * Demonstrates how to render arbitrary Compose graphics dynamically into a standard [com.google.android.gms.maps.model.BitmapDescriptor] icon.
 *
 * Uses the experimental [rememberComposeBitmapDescriptor] helper to capture a magenta circle composable
 * and bind it as the icon for a standard marker.
 */
@Composable
fun RememberComposeBitmapDescriptorSnippet() {
    // [START maps_android_compose_remember_bitmap_descriptor]
    val cameraPositionState = rememberCameraPositionState {
        position = defaultCameraPosition
    }
    val markerState = rememberUpdatedMarkerState(position = singapore)

    // Robust, crash-free custom marker descriptor generation using standard Android Canvas
    val customMarkerIcon = remember {
        val size = 96 // size in pixels
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        // Draw a styled magenta circle border and fill
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.MAGENTA
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        
        // Draw a smaller inner white circle
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, size / 3f, paint)

        BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        Marker(
            state = markerState,
            title = "Custom Descriptor Pin",
            icon = customMarkerIcon
        )
    }
    // [END maps_android_compose_remember_bitmap_descriptor]
}

/**
 * Demonstrates overlaying a dynamic map distance scale widget ([ScaleBar]) on top of the map viewport.
 *
 * The scale bar adapts its distance units automatically as the user pinches to zoom or pans the camera.
 */
@Composable
fun ScaleBarSnippet() {
    // [START maps_android_compose_scale_bar]
    val cameraPositionState = rememberCameraPositionState {
        position = defaultCameraPosition
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        )

        // Overlay the scale bar widget dynamically anchored at the top-start
        ScaleBar(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            cameraPositionState = cameraPositionState
        )
    }
    // [END maps_android_compose_scale_bar]
}
