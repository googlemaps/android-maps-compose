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

import com.google.android.gms.maps.model.UrlTileProvider
import java.net.MalformedURLException
import java.net.URL

/**
 * A [UrlTileProvider] for Web Map Service (WMS) layers that use the EPSG:3857 (Web Mercator)
 * projection.
 *
 * @param width the width of the tile in pixels.
 * @param height the height of the tile in pixels.
 * @param urlFormatter a lambda that returns the WMS URL for the given bounding box coordinates
 * (xMin, yMin, xMax, yMax) and zoom level.
 * @param datasetXMinBound the minimum X coordinate of the dataset in EPSG:3857 (default null).
 * @param datasetYMinBound the minimum Y coordinate of the dataset in EPSG:3857 (default null).
 * @param datasetXMaxBound the maximum X coordinate of the dataset in EPSG:3857 (default null).
 * @param datasetYMaxBound the maximum Y coordinate of the dataset in EPSG:3857 (default null).
 */
public class WmsUrlTileProvider(
    width: Int = 256,
    height: Int = 256,
    private val urlFormatter: (
        xMin: Double,
        yMin: Double,
        xMax: Double,
        yMax: Double,
        zoom: Int
    ) -> String,
    private val datasetXMinBound: Double? = null,
    private val datasetYMinBound: Double? = null,
    private val datasetXMaxBound: Double? = null,
    private val datasetYMaxBound: Double? = null,
) : UrlTileProvider(width, height) {
    private val bounded: Boolean = datasetXMinBound != null || datasetYMinBound != null || datasetXMaxBound != null || datasetYMaxBound != null

    override fun getTileUrl(x: Int, y: Int, zoom: Int): URL? {
        val bbox = getBoundingBox(x, y, zoom) // doubleArrayOf(xMin, yMin, xMax, yMax)
        // Return null if the tile is entirely outside the specified bounds of the dataset
        if(bounded && // skip checking for datasets where no bounds are specified
            (datasetXMaxBound != null && bbox[0] > datasetXMaxBound) || // xMin greater than datasets xMax. No overlap.
            (datasetYMaxBound != null && bbox[1] > datasetYMaxBound) || // yMin greater than datasets yMax. No overlap.
            (datasetXMinBound != null && bbox[2] < datasetXMinBound) || // xMax less than datasets xMin. No overlap.
            (datasetYMinBound != null && bbox[3] < datasetYMinBound) // yMax less than datasets yMin. No overlap.
        ){return null}
        val urlString = urlFormatter(bbox[0], bbox[1], bbox[2], bbox[3], zoom)
        return try {
            URL(urlString)
        } catch (e: MalformedURLException) {
            null
        }
    }

    private companion object {
        /**
         * The Earth's bound and circumference in meters at the equator according to EPSG:3857.
         */
        private const val BOUND = 20037508.34789244
        private const val EARTH_CIRCUMFERENCE = 2 * BOUND

    }

    /**
     * Calculates the bounding box for the given tile in EPSG:3857 coordinates.
     *
     * @return an array containing [xMin, yMin, xMax, yMax] in meters.
     */
    internal fun getBoundingBox(x: Int, y: Int, zoom: Int): DoubleArray {
        val numTiles: Int = 1 shl zoom // Powers of 2 are equivalent to bit-shifts
        val tileSizeMeters = EARTH_CIRCUMFERENCE / numTiles

        val xMin = -BOUND + (x * tileSizeMeters)
        val xMax = xMin + tileSizeMeters

        // Y is inverted in TMS/Google Maps tiles vs WMS BBOX
        // Top of map (y=0) is +20037508.34789244
        val yMax = BOUND - (y * tileSizeMeters)
        val yMin = yMax - tileSizeMeters

        return doubleArrayOf(xMin, yMin, xMax, yMax)
    }
}
