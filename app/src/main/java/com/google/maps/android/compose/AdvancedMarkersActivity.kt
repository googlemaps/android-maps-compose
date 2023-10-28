// Copyright 2023 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.maps.android.compose


import android.R.drawable.ic_menu_myplaces
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.PinConfig


private const val TAG = "AdvancedMarkersActivity"

private val santiago = LatLng(-33.4489, -70.6693)
private val bogota = LatLng(-4.7110, -74.0721)
private val lima = LatLng(-12.0464, -77.0428)
private val salvador = LatLng(-12.9777, -38.5016)
private val center = LatLng(-18.000, -58.000)
private val defaultCameraPosition1 = CameraPosition.fromLatLngZoom(center, 2f)
class AdvancedMarkersActivity : ComponentActivity(), OnMapsSdkInitializedCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapsInitializer.initialize(applicationContext, MapsInitializer.Renderer.LATEST, this)
        setContent {
            // Observing and controlling the camera's state can be done with a CameraPositionState
            val cameraPositionState = rememberCameraPositionState {
                position = defaultCameraPosition1
            }
            val mapProperties by remember {
                mutableStateOf(MapProperties(mapType = MapType.NORMAL))
            }
            val marker1State = rememberMarkerState(position = santiago)
            val marker2State = rememberMarkerState(position = bogota)
            val marker3State = rememberMarkerState(position = lima)
            val marker4State = rememberMarkerState(position = salvador)

            // Drawing on the map is accomplished with a child-based API
            val markerClick: (Marker) -> Boolean = {
                Log.d(TAG, "${it.title} was clicked")
                cameraPositionState.projection?.let { projection ->
                    Log.d(TAG, "The current projection is: $projection")
                }
                false
            }
            Box(Modifier.fillMaxSize()) {
                GoogleMap(
                    modifier = Modifier.matchParentSize(),
                    googleMapOptionsFactory = {
                        GoogleMapOptions().mapId("DEMO_MAP_ID")
                    },
                    cameraPositionState = cameraPositionState,
                    properties = mapProperties,
                    onPOIClick = {
                        Log.d(TAG, "POI clicked: ${it.name}")
                    }
                ) {

                    val textView = TextView(this@AdvancedMarkersActivity)
                    textView.text = "Hello!!"
                    textView.setBackgroundColor(Color.BLACK)
                    textView.setTextColor(Color.YELLOW)

                    AdvancedMarker(
                        state = marker4State,
                        onClick = markerClick,
                        collisionBehavior = 1,
                        iconView = textView,
                        title="Marker 4"
                    )

                    val pinConfig = PinConfig.builder()
                        .setBackgroundColor(Color.MAGENTA)
                        .setBorderColor(Color.WHITE)
                        .build()

                    AdvancedMarker(
                        state = marker1State,
                        onClick = markerClick,
                        collisionBehavior = 1,
                        pinConfig = pinConfig,
                        title="Marker 1"
                    )

                    val glyphOne = PinConfig.Glyph("A", Color.BLACK)
                    val pinConfig2 = PinConfig.builder()
                        .setGlyph(glyphOne)
                        .build()

                    AdvancedMarker(
                        state = marker2State,
                        onClick = markerClick,
                        collisionBehavior = 1,
                        pinConfig = pinConfig2,
                        title="Marker 2"
                    )

                    val glyphImage: Int = ic_menu_myplaces
                    val descriptor = BitmapDescriptorFactory.fromResource(glyphImage)
                    val pinConfig3 = PinConfig.builder()
                        .setGlyph(PinConfig.Glyph(descriptor))
                        .build()

                    AdvancedMarker(
                        state = marker3State,
                        onClick = markerClick,
                        collisionBehavior = 1,
                        pinConfig = pinConfig3,
                        title="Marker 3"
                    )

                }
            }
        }
    }

    override fun onMapsSdkInitialized(renderer: MapsInitializer.Renderer) {
        when (renderer) {
            MapsInitializer.Renderer.LATEST -> Log.d("MapsDemo", "The latest version of the renderer is used.")
            MapsInitializer.Renderer.LEGACY -> Log.d("MapsDemo", "The legacy version of the renderer is used.")
            else -> {}
        }
    }
}