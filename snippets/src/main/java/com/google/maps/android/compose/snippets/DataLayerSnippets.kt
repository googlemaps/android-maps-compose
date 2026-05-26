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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.data.geojson.GeoJsonLayer
import com.google.maps.android.data.kml.KmlLayer
import org.json.JSONObject
import java.io.ByteArrayInputStream
import kotlin.OptIn

/**
 * Demonstrates how to load and display a GeoJSON data layer on the map in Compose.
 *
 * This Composable uses [MapEffect] to get the raw [com.google.android.gms.maps.GoogleMap]
 * reference safely and instantiates a [GeoJsonLayer] with a self-contained GeoJSON JSONObject.
 */
@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun GeoJsonLayerSnippet() {
    // [START maps_android_compose_geojson_layer]
    val cameraPositionState = rememberCameraPositionState {
        position = defaultCameraPosition
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        // Use MapEffect to safely access the raw GoogleMap instance
        MapEffect(Unit) { googleMap ->
            val geoJsonData = JSONObject(
                """{
                    "type": "FeatureCollection",
                    "features": [
                      {
                        "type": "Feature",
                        "properties": {},
                        "geometry": {
                          "type": "Point",
                          "coordinates": [103.8742, 1.3588]
                        }
                      }
                    ]
                }"""
            )
            val geoJsonLayer = GeoJsonLayer(googleMap, geoJsonData)
            geoJsonLayer.addLayerToMap()
        }
    }
    // [END maps_android_compose_geojson_layer]
}

/**
 * Demonstrates how to load and display KML data layers on the map in Compose.
 *
 * This Composable uses [MapEffect] to obtain a reference to the raw GoogleMap instance,
 * parses a KML stream using [KmlLayer], and adds it onto the map viewport.
 */
@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun KmlLayerSnippet() {
    // [START maps_android_compose_kml_layer]
    val cameraPositionState = rememberCameraPositionState {
        position = defaultCameraPosition
    }
    val context = LocalContext.current

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        // Use MapEffect to safely access the raw GoogleMap instance
        MapEffect(Unit) { googleMap ->
            val kmlData = ByteArrayInputStream(
                """<?xml version="1.0" encoding="UTF-8"?>
                <kml xmlns="http://www.opengis.net/kml/2.2">
                  <Placemark>
                    <name>Singapore Point</name>
                    <Point>
                      <coordinates>103.8742114,1.3588227,0</coordinates>
                    </Point>
                  </Placemark>
                </kml>""".toByteArray()
            )
            val kmlLayer = KmlLayer(googleMap, kmlData, context)
            kmlLayer.addLayerToMap()
        }
    }
    // [END maps_android_compose_kml_layer]
}
