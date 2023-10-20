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


import android.R
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
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.PinConfig


private const val TAG = "AdvancedMarkersActivity"

class AdvancedMarkersActivity : ComponentActivity(), OnMapsSdkInitializedCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapsInitializer.initialize(applicationContext, MapsInitializer.Renderer.LATEST, this)
        setContent {
            // Observing and controlling the camera's state can be done with a CameraPositionState
            val cameraPositionState = rememberCameraPositionState {
                position = defaultCameraPosition
            }
            val mapProperties by remember {
                mutableStateOf(MapProperties(mapType = MapType.NORMAL))
            }
            val singaporeState = rememberMarkerState(position = singapore)
            val singapore2State = rememberMarkerState(position = singapore2)
            val singapore3State = rememberMarkerState(position = singapore3)
            val singapore4State = rememberMarkerState(position = singapore4)

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
                        GoogleMapOptions().mapId("45a7dec634a854b0")
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
                        state = singapore3State,
                        onClick = markerClick,
                        collisionBehavior = 1,
                        iconView = textView
                    )

                    val pinConfigBuilder = PinConfig.builder()
                    pinConfigBuilder.setBackgroundColor(Color.MAGENTA)
                    pinConfigBuilder.setBorderColor(resources.getColor(R.color.holo_orange_dark))

                    val pinConfig = pinConfigBuilder.build()

                    AdvancedMarker(
                        state = singapore2State,
                        onClick = markerClick,
                        collisionBehavior = 1,
                        pinConfig = pinConfig
                    )
                    val pinConfigBuilder2 = PinConfig.builder()
                    val glyphOne = PinConfig.Glyph("A", resources.getColor(R.color.black))
                    pinConfigBuilder2.setGlyph(glyphOne)

                    val pinConfig2 = pinConfigBuilder2.build()

                    AdvancedMarker(
                        state = singaporeState,
                        onClick = markerClick,
                        collisionBehavior = 1,
                        pinConfig = pinConfig2
                    )

                    val pinConfigBuilder3 = PinConfig.builder()
                    val glyphImage: Int = R.drawable.ic_menu_report_image
                    val descriptor = BitmapDescriptorFactory.fromResource(glyphImage)
                    pinConfigBuilder3.setGlyph(PinConfig.Glyph(descriptor))
                    val pinConfig3 = pinConfigBuilder3.build()

                    AdvancedMarker(
                        state = singapore4State,
                        onClick = markerClick,
                        collisionBehavior = 1,
                        pinConfig = pinConfig3
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