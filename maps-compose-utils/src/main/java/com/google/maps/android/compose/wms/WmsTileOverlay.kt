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

package com.google.maps.android.compose.wms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.android.gms.maps.model.TileOverlay
import com.google.maps.android.compose.TileOverlay
import com.google.maps.android.compose.TileOverlayState
import com.google.maps.android.compose.rememberTileOverlayState

/**
 * A Composable that displays a Web Map Service (WMS) layer using the EPSG:3857 projection.
 *
 * @param urlFormatter a lambda that returns the WMS URL for the given bounding box coordinates.
 * @param state the [TileOverlayState] to be used to control the tile overlay.
 * @param fadeIn boolean indicating whether the tiles should fade in.
 * @param transparency the transparency of the tile overlay.
 * @param visible the visibility of the tile overlay.
 * @param zIndex the z-index of the tile overlay.
 * @param onClick a lambda invoked when the tile overlay is clicked.
 * @param tileWidth the width of the tiles in pixels (default 256).
 * @param tileHeight the height of the tiles in pixels (default 256).
 * @param datasetXMinBound the minimum X coordinate of the dataset in EPSG:3857 (default null).
 * @param datasetYMinBound the minimum Y coordinate of the dataset in EPSG:3857 (default null).
 * @param datasetXMaxBound the maximum X coordinate of the dataset in EPSG:3857 (default null).
 * @param datasetYMaxBound the maximum Y coordinate of the dataset in EPSG:3857 (default null).
 */
@Composable
public fun WmsTileOverlay(
    urlFormatter: (xMin: Double, yMin: Double, xMax: Double, yMax: Double, zoom: Int) -> String,
    state: TileOverlayState = rememberTileOverlayState(),
    fadeIn: Boolean = true,
    transparency: Float = 0f,
    visible: Boolean = true,
    zIndex: Float = 0f,
    onClick: (TileOverlay) -> Unit = {},
    tileWidth: Int = 256,
    tileHeight: Int = 256,
    datasetXMinBound: Double? = null,
    datasetYMinBound: Double? = null,
    datasetXMaxBound: Double? = null,
    datasetYMaxBound: Double? = null
) {
    val tileProvider = remember(urlFormatter, tileWidth, tileHeight) {
        WmsUrlTileProvider(
            width = tileWidth,
            height = tileHeight,
            urlFormatter = urlFormatter,
            datasetXMinBound = datasetXMinBound,
            datasetYMinBound = datasetYMinBound,
            datasetXMaxBound = datasetXMaxBound,
            datasetYMaxBound = datasetYMaxBound
        )
    }
    TileOverlay(
        tileProvider = tileProvider,
        state = state,
        fadeIn = fadeIn,
        transparency = transparency,
        visible = visible,
        zIndex = zIndex,
        onClick = onClick
    )
}
