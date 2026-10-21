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

package com.google.maps.android.compose.data.kml

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
import com.google.maps.android.data.parser.kml.AndroidImageDecoder
import com.google.maps.android.data.parser.kml.ImageDecoder
import com.google.maps.android.data.parser.kml.KmlParser
import com.google.maps.android.data.parser.kml.KmzParser
import com.google.maps.android.data.renderer.IconProvider
import com.google.maps.android.data.renderer.UrlIconProvider
import com.google.maps.android.data.renderer.mapper.toLayer
import com.google.maps.android.data.renderer.model.DataLayer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

private const val DEFAULT_MAX_KMZ_ENTRY_COUNT = 200
private const val DEFAULT_MAX_KMZ_UNCOMPRESSED_SIZE = 50L * 1024L * 1024L

/**
 * Loads and parses KML or KMZ data from a raw Android resource and renders it onto the map using
 * the platform-agnostic [DataLayer] renderer.
 *
 * Automatically detects whether the stream is a plain KML document or a compressed KMZ archive and
 * enforces [maxKmzEntryCount] and [maxKmzUncompressedTotalSize] limits against zip-bomb payloads.
 *
 * @param rawResourceId the `@RawRes` resource identifier containing KML or KMZ data.
 * @param maxKmzEntryCount maximum number of ZIP entries permitted when parsing a KMZ archive.
 * @param maxKmzUncompressedTotalSize maximum total uncompressed byte size permitted when parsing a
 * KMZ archive.
 * @param iconProvider the [IconProvider] used to fetch remote marker or ground overlay icons.
 * @param useAdvancedMarkers whether to render point features using Advanced Markers.
 * @param ioDispatcher the [CoroutineDispatcher] used to read and parse the KML/KMZ resource.
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
public fun KmlLayer(
    @RawRes rawResourceId: Int,
    maxKmzEntryCount: Int = DEFAULT_MAX_KMZ_ENTRY_COUNT,
    maxKmzUncompressedTotalSize: Long = DEFAULT_MAX_KMZ_UNCOMPRESSED_SIZE,
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
    var loadedLayer by remember(
        rawResourceId,
        maxKmzEntryCount,
        maxKmzUncompressedTotalSize,
    ) {
        mutableStateOf<DataLayer?>(null)
    }

    LaunchedEffect(
        context,
        rawResourceId,
        maxKmzEntryCount,
        maxKmzUncompressedTotalSize,
        ioDispatcher,
    ) {
        val layer = withContext(ioDispatcher) {
            runCatching {
                context.resources.openRawResource(rawResourceId).use { stream ->
                    parseKmlOrKmzStream(
                        inputStream = stream,
                        maxKmzEntryCount = maxKmzEntryCount,
                        maxKmzUncompressedTotalSize = maxKmzUncompressedTotalSize,
                    )
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

/**
 * Loads and parses KML or KMZ data from an application asset file and renders it onto the map using
 * the platform-agnostic [DataLayer] renderer.
 *
 * Automatically detects whether the asset is a plain KML document or a compressed KMZ archive and
 * enforces [maxKmzEntryCount] and [maxKmzUncompressedTotalSize] limits against zip-bomb payloads.
 *
 * @param assetName the path of the KML or KMZ file in the application's `assets/` directory.
 * @param maxKmzEntryCount maximum number of ZIP entries permitted when parsing a KMZ archive.
 * @param maxKmzUncompressedTotalSize maximum total uncompressed byte size permitted when parsing a
 * KMZ archive.
 * @param iconProvider the [IconProvider] used to fetch remote marker or ground overlay icons.
 * @param useAdvancedMarkers whether to render point features using Advanced Markers.
 * @param ioDispatcher the [CoroutineDispatcher] used to read and parse the KML/KMZ asset.
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
public fun KmlLayer(
    assetName: String,
    maxKmzEntryCount: Int = DEFAULT_MAX_KMZ_ENTRY_COUNT,
    maxKmzUncompressedTotalSize: Long = DEFAULT_MAX_KMZ_UNCOMPRESSED_SIZE,
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
    var loadedLayer by remember(
        assetName,
        maxKmzEntryCount,
        maxKmzUncompressedTotalSize,
    ) {
        mutableStateOf<DataLayer?>(null)
    }

    LaunchedEffect(
        context,
        assetName,
        maxKmzEntryCount,
        maxKmzUncompressedTotalSize,
        ioDispatcher,
    ) {
        val layer = withContext(ioDispatcher) {
            runCatching {
                context.assets.open(assetName).use { stream ->
                    parseKmlOrKmzStream(
                        inputStream = stream,
                        maxKmzEntryCount = maxKmzEntryCount,
                        maxKmzUncompressedTotalSize = maxKmzUncompressedTotalSize,
                    )
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

internal fun parseKmlOrKmzStream(
    inputStream: InputStream,
    maxKmzEntryCount: Int = DEFAULT_MAX_KMZ_ENTRY_COUNT,
    maxKmzUncompressedTotalSize: Long = DEFAULT_MAX_KMZ_UNCOMPRESSED_SIZE,
    imageDecoder: ImageDecoder = AndroidImageDecoder(),
): DataLayer {
    val buffered = BufferedInputStream(inputStream)
    buffered.mark(1024)
    val headerBytes = ByteArray(1024)
    val read = buffered.read(headerBytes)
    buffered.reset()

    val isKmz =
        read > 0 && KmzParser.canParse(String(headerBytes, 0, read, StandardCharsets.UTF_8))
    val kml = if (isKmz) {
        KmzParser(
            imageDecoder = imageDecoder,
            maxKmzEntryCount = maxKmzEntryCount,
            maxKmzUncompressedTotalSize = maxKmzUncompressedTotalSize,
        ).parse(buffered)
    } else {
        KmlParser().parse(buffered)
    }
    return kml.toLayer()
}
