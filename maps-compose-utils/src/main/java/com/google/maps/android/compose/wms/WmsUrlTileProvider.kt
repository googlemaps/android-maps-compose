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
import kotlin.math.pow

/**
 * A [UrlTileProvider] for Web Map Service (WMS) layers that use the EPSG:3857 (Web Mercator)
 * projection.
 *
 * @param width the width of the tile in pixels.
 * @param height the height of the tile in pixels.
 * @param urlFormatter a lambda that returns the WMS URL for the given bounding box coordinates
 * (xMin, yMin, xMax, yMax) and zoom level.
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
    ) -> String
) : UrlTileProvider(width, height) {

    override fun getTileUrl(x: Int, y: Int, zoom: Int): URL? {
        val bbox = getBoundingBox(x, y, zoom)
        val urlString = urlFormatter(bbox[0], bbox[1], bbox[2], bbox[3], zoom)
        return try {
            URL(urlString)
        } catch (e: MalformedURLException) {
            null
        }
    }

    private companion object {
        /**
         * The Earth's circumference in meters at the equator according to EPSG:3857.
         */
        private const val EARTH_CIRCUMFERENCE = 2 * 20037508.34789244
    }

    /**
     * Calculates the bounding box for the given tile in EPSG:3857 coordinates.
     *
     * @return an array containing [xMin, yMin, xMax, yMax] in meters.
     */
    internal fun getBoundingBox(x: Int, y: Int, zoom: Int): DoubleArray {
        val numTiles = 2.0.pow(zoom.toDouble())
        val tileSizeMeters = EARTH_CIRCUMFERENCE / numTiles

        val xMin = -20037508.34789244 + (x * tileSizeMeters)
        val xMax = -20037508.34789244 + ((x + 1) * tileSizeMeters)

        // Y is inverted in TMS/Google Maps tiles vs WMS BBOX
        // Top of map (y=0) is +20037508.34789244
        val yMax = 20037508.34789244 - (y * tileSizeMeters)
        val yMin = 20037508.34789244 - ((y + 1) * tileSizeMeters)

        return doubleArrayOf(xMin, yMin, xMax, yMax)
    }
}
