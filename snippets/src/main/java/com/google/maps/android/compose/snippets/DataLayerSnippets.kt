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
import java.io.ByteArrayInputStream
import kotlin.OptIn
import org.json.JSONObject

/**
 * Demonstrates how to load and display a GeoJSON data layer (representing a Polyline) on the map.
 *
 * This Composable uses [MapEffect] to get the raw [com.google.android.gms.maps.GoogleMap] reference
 * safely and instantiates a [GeoJsonLayer] with a self-contained GeoJSON LineString.
 */
@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun GeoJsonLayerSnippet() {
  // [START maps_android_compose_geojson_layer]
  val cameraPositionState = rememberCameraPositionState { position = defaultCameraPosition }

  GoogleMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState) {
    // Use MapEffect to safely access the raw GoogleMap instance
    MapEffect(Unit) { googleMap ->
      val geoJsonData =
        JSONObject(
          """{
                    "type": "FeatureCollection",
                    "features": [
                      {
                        "type": "Feature",
                        "properties": {},
                        "geometry": {
                          "type": "LineString",
                          "coordinates": [
                            [103.80, 1.35],
                            [103.85, 1.40],
                            [103.90, 1.35]
                          ]
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
 * Demonstrates how to load and display KML data layers (representing a Polygon area) on the map.
 *
 * Parses a KML stream defining a closed triangular area over Singapore using [KmlLayer], rendering
 * it dynamically on the map.
 */
@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun KmlLayerSnippet() {
  // [START maps_android_compose_kml_layer]
  val cameraPositionState = rememberCameraPositionState { position = defaultCameraPosition }
  val context = LocalContext.current

  GoogleMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState) {
    // Use MapEffect to safely access the raw GoogleMap instance
    MapEffect(Unit) { googleMap ->
      val kmlData =
        ByteArrayInputStream(
          """<?xml version="1.0" encoding="UTF-8"?>
                <kml xmlns="http://www.opengis.net/kml/2.2">
                  <Placemark>
                    <name>KML Polygon Area</name>
                    <Polygon>
                      <outerBoundaryIs>
                        <LinearRing>
                          <coordinates>
                            103.80,1.30,0
                            103.85,1.38,0
                            103.90,1.30,0
                            103.80,1.30,0
                          </coordinates>
                        </LinearRing>
                      </outerBoundaryIs>
                    </Polygon>
                  </Placemark>
                </kml>"""
            .toByteArray()
        )
      val kmlLayer = KmlLayer(googleMap, kmlData, context)
      kmlLayer.addLayerToMap()
    }
  }
  // [END maps_android_compose_kml_layer]
}
