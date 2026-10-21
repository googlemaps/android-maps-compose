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

package com.google.maps.android.compose.data

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.rememberReattachClickListenersHandle
import com.google.maps.android.data.DataLayerLoader
import com.google.maps.android.data.renderer.IconProvider
import com.google.maps.android.data.renderer.UrlIconProvider
import com.google.maps.android.data.renderer.mapview.MapViewRenderer
import com.google.maps.android.data.renderer.model.DataLayer
import com.google.maps.android.data.renderer.model.DataScene
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders a platform-agnostic [DataScene] onto the map using [MapViewRenderer].
 *
 * When [scene] changes or the composable leaves composition, previously rendered features are
 * removed from the map.
 *
 * @param scene the [DataScene] to render.
 * @param iconProvider the [IconProvider] used to fetch remote marker or ground overlay icons.
 * @param useAdvancedMarkers whether to render point features using Advanced Markers.
 * @param markerManager an optional [MarkerManager] to manage rendered markers.
 * @param polygonManager an optional [PolygonManager] to manage rendered polygons.
 * @param polylineManager an optional [PolylineManager] to manage rendered polylines.
 * @param groundOverlayManager an optional [GroundOverlayManager] to manage rendered ground overlays.
 */
@Composable
@GoogleMapComposable
@MapsComposeExperimentalApi
public fun DataScene(
    scene: DataScene,
    iconProvider: IconProvider = remember { UrlIconProvider() },
    useAdvancedMarkers: Boolean = false,
    markerManager: MarkerManager? = null,
    polygonManager: PolygonManager? = null,
    polylineManager: PolylineManager? = null,
    groundOverlayManager: GroundOverlayManager? = null,
) {
    val reattach = rememberReattachClickListenersHandle()
    var renderer by remember { mutableStateOf<MapViewRenderer?>(null) }

    MapEffect(
        iconProvider,
        markerManager,
        polygonManager,
        polylineManager,
        groundOverlayManager,
    ) { map ->
        val newRenderer = MapViewRenderer(
            map = map,
            iconProvider = iconProvider,
            markerManager = markerManager,
            polygonManager = polygonManager,
            polylineManager = polylineManager,
            groundOverlayManager = groundOverlayManager,
        )
        renderer = newRenderer
        if (
            markerManager != null ||
            polygonManager != null ||
            polylineManager != null ||
            groundOverlayManager != null
        ) {
            Handler(Looper.getMainLooper()).post {
                reattach()
            }
        }
    }

    val currentRenderer = renderer ?: return

    DisposableEffect(currentRenderer, scene, useAdvancedMarkers) {
        currentRenderer.useAdvancedMarkers = useAdvancedMarkers
        renderSceneWithCachedImages(currentRenderer, scene)
        onDispose {
            currentRenderer.clear()
        }
    }
}

/**
 * Renders a platform-agnostic [DataLayer] (parsed from GeoJSON, KML, KMZ, or GPX) onto the map
 * using [MapViewRenderer].
 *
 * @param layer the [DataLayer] to render.
 * @param iconProvider the [IconProvider] used to fetch remote marker or ground overlay icons.
 * @param useAdvancedMarkers whether to render point features using Advanced Markers.
 * @param markerManager an optional [MarkerManager] to manage rendered markers.
 * @param polygonManager an optional [PolygonManager] to manage rendered polygons.
 * @param polylineManager an optional [PolylineManager] to manage rendered polylines.
 * @param groundOverlayManager an optional [GroundOverlayManager] to manage rendered ground overlays.
 */
@Composable
@GoogleMapComposable
@MapsComposeExperimentalApi
public fun DataLayer(
    layer: DataLayer,
    iconProvider: IconProvider = remember { UrlIconProvider() },
    useAdvancedMarkers: Boolean = false,
    markerManager: MarkerManager? = null,
    polygonManager: PolygonManager? = null,
    polylineManager: PolylineManager? = null,
    groundOverlayManager: GroundOverlayManager? = null,
) {
    val scene = remember(layer) { DataScene(listOf(layer)) }
    DataScene(
        scene = scene,
        iconProvider = iconProvider,
        useAdvancedMarkers = useAdvancedMarkers,
        markerManager = markerManager,
        polygonManager = polygonManager,
        polylineManager = polylineManager,
        groundOverlayManager = groundOverlayManager,
    )
}

/**
 * Loads a [DataLayer] from an application asset file (supporting `.geojson`, `.json`, `.kml`,
 * `.kmz`, and `.gpx`) using [DataLayerLoader] and renders it onto the map.
 *
 * @param assetName the path of the asset file in the application's `assets/` directory.
 * @param iconProvider the [IconProvider] used to fetch remote marker or ground overlay icons.
 * @param useAdvancedMarkers whether to render point features using Advanced Markers.
 * @param ioDispatcher the [CoroutineDispatcher] used for reading and parsing the asset file.
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
public fun DataLayer(
    assetName: String,
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
    var loadedLayer by remember(assetName) { mutableStateOf<DataLayer?>(null) }

    LaunchedEffect(context, assetName, ioDispatcher) {
        val layer = DataLayerLoader(context, ioDispatcher).loadAsset(assetName)
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
 * Loads a [DataLayer] from a raw Android resource (supporting GeoJSON, KML, KMZ, and GPX) using
 * [DataLayerLoader] and renders it onto the map.
 *
 * @param rawResourceId the `@RawRes` resource identifier to load.
 * @param fileName an optional file name hint (e.g., `"boroughs.geojson"` or `"campus.kmz"`) used by
 * [DataLayerLoader] for extension matching before falling back to content sniffing.
 * @param iconProvider the [IconProvider] used to fetch remote marker or ground overlay icons.
 * @param useAdvancedMarkers whether to render point features using Advanced Markers.
 * @param ioDispatcher the [CoroutineDispatcher] used for reading and parsing the raw resource.
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
public fun DataLayer(
    @RawRes rawResourceId: Int,
    fileName: String? = null,
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
    var loadedLayer by remember(rawResourceId, fileName) { mutableStateOf<DataLayer?>(null) }

    LaunchedEffect(context, rawResourceId, fileName, ioDispatcher) {
        val layer = withContext(ioDispatcher) {
            runCatching {
                context.resources.openRawResource(rawResourceId).use { stream ->
                    DataLayerLoader(context, ioDispatcher).loadInputStream(stream, fileName)
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

internal fun renderSceneWithCachedImages(renderer: MapViewRenderer, scene: DataScene) {
    scene.layers.forEach { layer ->
        val images = layer.properties["images"] as? Map<*, *>
        images?.forEach { (key, value) ->
            if (key is String && value is Bitmap) {
                renderer.cacheImageData(key, value)
            }
        }
    }
    renderer.render(scene)
}
