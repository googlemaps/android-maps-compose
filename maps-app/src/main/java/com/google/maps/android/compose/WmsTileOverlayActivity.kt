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

package com.google.maps.android.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.wms.WmsTileOverlay

/**
 * This activity demonstrates how to use [WmsTileOverlay] to display a Web Map Service (WMS)
 * layer on a map.
 */
class WmsTileOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val center = LatLng(39.50, -98.35) // Center of US
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(center, 4f)
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                // Example: USGS National Map Shaded Relief (WMS)
                WmsTileOverlay(
                    urlFormatter = { xMin, yMin, xMax, yMax, _ ->
                        "https://basemap.nationalmap.gov/arcgis/services/USGSShadedReliefOnly/MapServer/WmsServer?" +
                            "SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap" +
                            "&FORMAT=image/png&TRANSPARENT=true&LAYERS=0" +
                            "&SRS=EPSG:3857&WIDTH=256&HEIGHT=256" +
                            "&BBOX=$xMin,$yMin,$xMax,$yMax"
                    },
                    transparency = 0.5f
                )
            }
        }
    }
}
