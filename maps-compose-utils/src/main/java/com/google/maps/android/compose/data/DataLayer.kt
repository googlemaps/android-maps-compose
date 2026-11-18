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
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import com.google.android.gms.maps.GoogleMap
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
import kotlinx.coroutines.awaitCancellation
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
public fun DataSceneOverlay(
    scene: DataScene,
    iconProvider: IconProvider = remember { UrlIconProvider() },
    useAdvancedMarkers: Boolean = false,
    markerManager: MarkerManager? = null,
    polygonManager: PolygonManager? = null,
    polylineManager: PolylineManager? = null,
    groundOverlayManager: GroundOverlayManager? = null,
) {
    val reattach = rememberReattachClickListenersHandle()

    MapEffect(
        iconProvider,
        markerManager,
        polygonManager,
        polylineManager,
        groundOverlayManager,
        scene,
        useAdvancedMarkers,
    ) { map ->
        val renderer = createAndRenderScene(
            map = map,
            scene = scene,
            iconProvider = iconProvider,
            useAdvancedMarkers = useAdvancedMarkers,
            markerManager = markerManager,
            polygonManager = polygonManager,
            polylineManager = polylineManager,
            groundOverlayManager = groundOverlayManager,
            reattach = reattach,
        )
        try {
            awaitCancellation()
        } finally {
            renderer.clear()
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
public fun DataLayerOverlay(
    layer: DataLayer,
    iconProvider: IconProvider = remember { UrlIconProvider() },
    useAdvancedMarkers: Boolean = false,
    markerManager: MarkerManager? = null,
    polygonManager: PolygonManager? = null,
    polylineManager: PolylineManager? = null,
    groundOverlayManager: GroundOverlayManager? = null,
) {
    val scene = remember(layer) { DataScene(listOf(layer)) }
    DataSceneOverlay(
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
 * Asynchronously loads and remembers a [DataLayer] from an application asset file (supporting
 * `.geojson`, `.json`, `.kml`, `.kmz`, and `.gpx`) using [DataLayerLoader].
 *
 * While a new [assetName] is loading, the previously loaded [DataLayer] (if any) is retained to
 * prevent visual flickering until parsing completes.
 *
 * @param assetName the path of the asset file in the application's `assets/` directory.
 * @param ioDispatcher the [CoroutineDispatcher] used for reading and parsing the asset file.
 * Custom dispatcher instances must be remembered across recompositions.
 * @param onError an optional callback invoked with any [Throwable] encountered during loading or parsing.
 * @param onLayerLoaded an optional callback invoked with the parsed [DataLayer] (or `null` if
 * parsing failed).
 * @return the loaded [DataLayer], or `null` while loading or if loading failed and no previous
 * layer was loaded.
 */
@Composable
@MapsComposeExperimentalApi
public fun rememberDataLayer(
    assetName: String,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    onError: ((Throwable) -> Unit)? = null,
    onLayerLoaded: (DataLayer?) -> Unit = {},
): DataLayer? {
    val context = LocalContext.current
    val onLayerLoadedState by rememberUpdatedState(onLayerLoaded)
    val onErrorState by rememberUpdatedState(onError)
    var loadedLayer by remember { mutableStateOf<DataLayer?>(null) }

    LaunchedEffect(context, assetName, ioDispatcher) {
        loadLayerWithCallbacks(
            defaultErrorMessage = "Failed to load DataLayer from asset: $assetName",
            onLayerLoaded = onLayerLoadedState,
            onError = onErrorState,
        ) {
            DataLayerLoader(context, ioDispatcher).loadAsset(assetName)
        }?.let { loadedLayer = it }
    }

    return loadedLayer
}

/**
 * Asynchronously loads and remembers a [DataLayer] from a raw Android resource (supporting
 * GeoJSON, KML, KMZ, and GPX) using [DataLayerLoader].
 *
 * While a new [rawResourceId] is loading, the previously loaded [DataLayer] (if any) is retained to
 * prevent visual flickering until parsing completes.
 *
 * @param rawResourceId the `@RawRes` resource identifier to load.
 * @param fileName an optional file name hint (e.g., `"boroughs.geojson"` or `"campus.kmz"`) used by
 * [DataLayerLoader] for extension matching before falling back to content sniffing.
 * @param ioDispatcher the [CoroutineDispatcher] used for reading and parsing the raw resource.
 * Custom dispatcher instances must be remembered across recompositions.
 * @param onError an optional callback invoked with any [Throwable] encountered during loading or parsing.
 * @param onLayerLoaded an optional callback invoked with the parsed [DataLayer] (or `null` if
 * parsing failed).
 * @return the loaded [DataLayer], or `null` while loading or if loading failed and no previous
 * layer was loaded.
 */
@Composable
@MapsComposeExperimentalApi
public fun rememberDataLayer(
    @RawRes rawResourceId: Int,
    fileName: String? = null,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    onError: ((Throwable) -> Unit)? = null,
    onLayerLoaded: (DataLayer?) -> Unit = {},
): DataLayer? {
    val context = LocalContext.current
    val resources = LocalResources.current
    val onLayerLoadedState by rememberUpdatedState(onLayerLoaded)
    val onErrorState by rememberUpdatedState(onError)
    var loadedLayer by remember { mutableStateOf<DataLayer?>(null) }

    LaunchedEffect(context, resources, rawResourceId, fileName, ioDispatcher) {
        loadLayerWithCallbacks(
            defaultErrorMessage = "Failed to load DataLayer from raw resource: $rawResourceId",
            onLayerLoaded = onLayerLoadedState,
            onError = onErrorState,
        ) {
            withContext(ioDispatcher) {
                resources.openRawResource(rawResourceId).use { stream ->
                    DataLayerLoader(context, ioDispatcher).loadInputStream(stream, fileName)
                }
            }
        }?.let { loadedLayer = it }
    }

    return loadedLayer
}

internal suspend fun loadLayerWithCallbacks(
    defaultErrorMessage: String,
    onLayerLoaded: (DataLayer?) -> Unit,
    onError: ((Throwable) -> Unit)?,
    loaderBlock: suspend () -> DataLayer?,
): DataLayer? {
    val result = runCatching { loaderBlock() }
    val layer = result.getOrNull()
    return if (layer != null) {
        onLayerLoaded(layer)
        layer
    } else {
        val error = result.exceptionOrNull() ?: IllegalStateException(defaultErrorMessage)
        onError?.invoke(error)
        onLayerLoaded(null)
        null
    }
}

internal fun createAndRenderScene(
    map: GoogleMap,
    scene: DataScene,
    iconProvider: IconProvider,
    useAdvancedMarkers: Boolean,
    markerManager: MarkerManager?,
    polygonManager: PolygonManager?,
    polylineManager: PolylineManager?,
    groundOverlayManager: GroundOverlayManager?,
    reattach: () -> Unit,
    rendererFactory: (GoogleMap) -> MapViewRenderer = { targetMap ->
        MapViewRenderer(
            map = targetMap,
            iconProvider = iconProvider,
            markerManager = markerManager,
            polygonManager = polygonManager,
            polylineManager = polylineManager,
            groundOverlayManager = groundOverlayManager,
        )
    },
): MapViewRenderer {
    val renderer = rendererFactory(map).apply {
        this.useAdvancedMarkers = useAdvancedMarkers
    }
    renderSceneWithCachedImages(renderer, scene)
    if (
        markerManager != null ||
        polygonManager != null ||
        polylineManager != null ||
        groundOverlayManager != null
    ) {
        reattach()
    }
    return renderer
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
