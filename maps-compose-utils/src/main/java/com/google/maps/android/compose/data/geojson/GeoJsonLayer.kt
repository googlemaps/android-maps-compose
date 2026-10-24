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

package com.google.maps.android.compose.data.geojson

import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.maps.android.collections.GroundOverlayManager
import com.google.maps.android.collections.MarkerManager
import com.google.maps.android.collections.PolygonManager
import com.google.maps.android.collections.PolylineManager
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.data.DataLayer
import com.google.maps.android.data.parser.geojson.GeoJsonParser
import com.google.maps.android.data.renderer.IconProvider
import com.google.maps.android.data.renderer.UrlIconProvider
import com.google.maps.android.data.renderer.mapper.toLayer
import com.google.maps.android.data.renderer.model.DataLayer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Parses a [JSONObject] containing GeoJSON data and renders it onto the map using the
 * platform-agnostic [DataLayer] renderer.
 *
 * @param geoJsonData the [JSONObject] containing GeoJSON features or geometry.
 * @param iconProvider the [IconProvider] used to fetch remote marker icons.
 * @param useAdvancedMarkers whether to render point features using Advanced Markers.
 * @param ioDispatcher the [CoroutineDispatcher] used to parse the GeoJSON payload.
 * @param markerManager an optional [MarkerManager] to manage rendered markers.
 * @param polygonManager an optional [PolygonManager] to manage rendered polygons.
 * @param polylineManager an optional [PolylineManager] to manage rendered polylines.
 * @param groundOverlayManager an optional [GroundOverlayManager] to manage rendered ground overlays.
 * @param onLayerLoaded an optional callback invoked with the parsed [DataLayer] (or `null` if
 * parsing failed).
 */
@Composable
@GoogleMapComposable
@MapsComposeExperimentalApi
public fun GeoJsonLayer(
    geoJsonData: JSONObject,
    iconProvider: IconProvider = remember { UrlIconProvider() },
    useAdvancedMarkers: Boolean = false,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    markerManager: MarkerManager? = null,
    polygonManager: PolygonManager? = null,
    polylineManager: PolylineManager? = null,
    groundOverlayManager: GroundOverlayManager? = null,
    onLayerLoaded: (DataLayer?) -> Unit = {},
) {
    val onLayerLoadedState by rememberUpdatedState(onLayerLoaded)
    var loadedLayer by remember(geoJsonData) { mutableStateOf<DataLayer?>(null) }

    LaunchedEffect(geoJsonData, ioDispatcher) {
        val layer = withContext(ioDispatcher) {
            parseGeoJsonLayer(geoJsonData.toString())
        }
        loadedLayer = layer
        onLayerLoadedState(layer)
    }

    loadedLayer?.let { layer ->
        DataLayer(
            layer = layer,
            iconProvider = iconProvider,
            useAdvancedMarkers = useAdvancedMarkers,
            markerManager = markerManager,
            polygonManager = polygonManager,
            polylineManager = polylineManager,
            groundOverlayManager = groundOverlayManager,
        )
    }
}

/**
 * Loads and parses GeoJSON data from a raw Android resource and renders it onto the map using the
 * platform-agnostic [DataLayer] renderer.
 *
 * @param rawResourceId the `@RawRes` resource identifier containing GeoJSON data.
 * @param iconProvider the [IconProvider] used to fetch remote marker icons.
 * @param useAdvancedMarkers whether to render point features using Advanced Markers.
 * @param ioDispatcher the [CoroutineDispatcher] used to read and parse the GeoJSON resource.
 * @param markerManager an optional [MarkerManager] to manage rendered markers.
 * @param polygonManager an optional [PolygonManager] to manage rendered polygons.
 * @param polylineManager an optional [PolylineManager] to manage rendered polylines.
 * @param groundOverlayManager an optional [GroundOverlayManager] to manage rendered ground overlays.
 * @param onLayerLoaded an optional callback invoked with the parsed [DataLayer] (or `null` if
 * parsing failed).
 */
@Composable
@GoogleMapComposable
@MapsComposeExperimentalApi
public fun GeoJsonLayer(
    @RawRes rawResourceId: Int,
    iconProvider: IconProvider = remember { UrlIconProvider() },
    useAdvancedMarkers: Boolean = false,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    markerManager: MarkerManager? = null,
    polygonManager: PolygonManager? = null,
    polylineManager: PolylineManager? = null,
    groundOverlayManager: GroundOverlayManager? = null,
    onLayerLoaded: (DataLayer?) -> Unit = {},
) {
    val context = LocalContext.current
    val onLayerLoadedState by rememberUpdatedState(onLayerLoaded)
    var loadedLayer by remember(rawResourceId) { mutableStateOf<DataLayer?>(null) }

    LaunchedEffect(context, rawResourceId, ioDispatcher) {
        val layer = withContext(ioDispatcher) {
            runCatching {
                context.resources.openRawResource(rawResourceId).use { stream ->
                    GeoJsonParser().parse(stream)?.toLayer()
                }
            }.getOrNull()
        }
        loadedLayer = layer
        onLayerLoadedState(layer)
    }

    loadedLayer?.let { layer ->
        DataLayer(
            layer = layer,
            iconProvider = iconProvider,
            useAdvancedMarkers = useAdvancedMarkers,
            markerManager = markerManager,
            polygonManager = polygonManager,
            polylineManager = polylineManager,
            groundOverlayManager = groundOverlayManager,
        )
    }
}

internal fun parseGeoJsonLayer(jsonString: String): DataLayer? = runCatching {
    jsonString.byteInputStream().use { stream ->
        GeoJsonParser().parse(stream)?.toLayer()
    }
}.getOrNull()
