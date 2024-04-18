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

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.StreetViewPanoramaOptions
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.Status
import com.google.maps.android.compose.streetview.StreetView
import com.google.maps.android.compose.streetview.rememberStreetViewCameraPositionState
import com.google.maps.android.ktx.MapsExperimentalFeature
import kotlinx.coroutines.launch
import com.google.maps.android.StreetViewUtils.Companion.fetchStreetViewData

class StreetViewActivity : ComponentActivity() {

    private val TAG = StreetViewActivity::class.java.simpleName

    // This is an invalid location. If you use it instead of Singapore, the StreetViewUtils
    // will return NOT_FOUND.
    val invalidLocation = LatLng(32.429634, -96.828891)

    @OptIn(MapsExperimentalFeature::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var isPanningEnabled by remember { mutableStateOf(false) }
            var isZoomEnabled by remember { mutableStateOf(false) }
            var streetViewResult by remember { mutableStateOf(Status.NOT_FOUND) }

            val camera = rememberStreetViewCameraPositionState()
            LaunchedEffect(camera) {
                launch {
                    snapshotFlow { camera.panoramaCamera }
                        .collect {
                            Log.d(TAG, "Camera at: $it")
                        }
                }
                launch {
                    snapshotFlow { camera.location }
                        .collect {
                            Log.d(TAG, "Location at: $it")
                        }
                }
                launch {
                    // Be sure to enable the Street View Static API on the project associated with
                    // this API key using the instructions at https://goo.gle/enable-sv-static-api
                    streetViewResult =
                        fetchStreetViewData(singapore, BuildConfig.MAPS_API_KEY)
                }
            }
            Box(Modifier.fillMaxSize(), Alignment.BottomStart) {
                if (streetViewResult == Status.OK) {
                    StreetView(
                        Modifier.matchParentSize(),
                        cameraPositionState = camera,
                        streetViewPanoramaOptionsFactory = {
                            StreetViewPanoramaOptions().position(singapore)
                        },
                        isPanningGesturesEnabled = isPanningEnabled,
                        isZoomGesturesEnabled = isZoomEnabled,
                        onClick = {
                            Log.d(TAG, "Street view clicked")
                        },
                        onLongClick = {
                            Log.d(TAG, "Street view long clicked")
                        }
                    )
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(8.dp)
                    ) {
                        StreetViewSwitch(title = "Panning", checked = isPanningEnabled) {
                            isPanningEnabled = it
                        }
                        StreetViewSwitch(title = "Zooming", checked = isZoomEnabled) {
                            isZoomEnabled = it
                        }
                    }
                } else {
                    Text("Location not available.")
                }
            }
        }
    }
}


@Composable
fun StreetViewSwitch(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.padding(4.dp)) {
        Text(title)
        Spacer(Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}