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

package com.google.maps.android.compose.markerexamples


import android.R.drawable.ic_menu_myplaces
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
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
import com.google.maps.android.compose.AdvancedMarker
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.google.maps.android.ui.IconGenerator


private const val TAG = "AdvancedMarkersActivity"

private val santiago = LatLng(-33.4489, -70.6693)
private val bogota = LatLng(-4.7110, -74.0721)
private val lima = LatLng(-12.0464, -77.0428)
private val salvador = LatLng(-12.9777, -38.5016)
private val caracas = LatLng(10.4785, -66.9016)
private val center = LatLng(-18.000, -58.000)
private val defaultCameraPosition1 = CameraPosition.fromLatLngZoom(center, 2f)
class AdvancedMarkersActivity : ComponentActivity(), OnMapsSdkInitializedCallback {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Observing and controlling the camera's state can be done with a CameraPositionState
            val cameraPositionState = rememberCameraPositionState {
                position = defaultCameraPosition1
            }
            val mapProperties by remember {
                mutableStateOf(MapProperties(mapType = MapType.NORMAL))
            }
            val marker1State = rememberUpdatedMarkerState(position = santiago)
            val marker2State = rememberUpdatedMarkerState(position = bogota)
            val marker3State = rememberUpdatedMarkerState(position = lima)
            val marker4State = rememberUpdatedMarkerState(position = salvador)
            val marker5State = rememberUpdatedMarkerState(position = caracas)

            // Drawing on the map is accomplished with a child-based API
            val markerClick: (Marker) -> Boolean = {
                Log.d(TAG, "${it.title} was clicked")
                cameraPositionState.projection?.let { projection ->
                    Log.d(TAG, "The current projection is: $projection")
                }
                false
            }
            Box(
                modifier = Modifier.fillMaxSize()
                    .systemBarsPadding(),
            ) {
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

                    val icon = remember {
                        val iconGenerator = IconGenerator(this@AdvancedMarkersActivity)
                        val contentView = TextView(this@AdvancedMarkersActivity)
                        contentView.text = "Caracas"
                        contentView.setBackgroundColor(Color.BLACK)
                        contentView.setTextColor(Color.YELLOW)
                        iconGenerator.setBackground(null)
                        iconGenerator.setContentView(contentView)
                        val bitmap = iconGenerator.makeIcon()
                        BitmapDescriptorFactory.fromBitmap(bitmap)
                    }
                    AdvancedMarker(
                        state = marker5State,
                        onClick = markerClick,
                        collisionBehavior = 1,
                        icon = icon,
                        title = "Marker 5"
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
        }
    }
}