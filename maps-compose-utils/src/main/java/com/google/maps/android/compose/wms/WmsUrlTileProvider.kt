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
import kotlin.math.PI

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
         * The maximum extent of the Web Mercator projection (EPSG:3857) in meters.
         * This is the distance from the origin (0,0) to the edge of the world map.
         * Calculated as semi-major axis of Earth (6378137.0) * PI.
         */
        private const val WORLD_EXTENT = (6378137.0) * PI

        /**
         * The total width/height of the world map in meters.
         */
        private const val WORLD_SIZE_METERS = 2 * WORLD_EXTENT
    }

    /**
     * Calculates the bounding box for the given tile in EPSG:3857 coordinates.
     *
     * @return an array containing [xMin, yMin, xMax, yMax] in meters.
     */
    internal fun getBoundingBox(x: Int, y: Int, zoom: Int): DoubleArray {
        // 1. Calculate how many tiles exist in each dimension at this zoom level (2^zoom).
        val tilesPerDimension = 1 shl zoom
        
        // 2. Divide the total world span by the number of tiles to find the metric size of one tile.
        val tileSizeMeters = WORLD_SIZE_METERS / tilesPerDimension.toDouble()

        // 3. X-axis: Starts at the far left (-WORLD_EXTENT) and moves East.
        val xMin = -WORLD_EXTENT + (x * tileSizeMeters)
        val xMax = -WORLD_EXTENT + ((x + 1) * tileSizeMeters)

        // 4. Y-axis: Google Maps/TMS starts at the Top (y=0 is North) and moves South.
        // WMS Bounding Box expects yMin to be the southern-most latitude and yMax to be the northern-most.
        // Therefore, we subtract the tile distance from the northern-most edge (+WORLD_EXTENT).
        val yMax = WORLD_EXTENT - (y * tileSizeMeters)
        val yMin = WORLD_EXTENT - ((y + 1) * tileSizeMeters)

        return doubleArrayOf(xMin, yMin, xMax, yMax)
    }
}
