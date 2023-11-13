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
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import kotlinx.coroutines.launch


class CustomControlsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var isMapLoaded by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()
            // This needs to be manually deactivated to avoid having a custom and the native
            // location button
            val uiSettings by remember {
                mutableStateOf(
                    MapUiSettings(
                        myLocationButtonEnabled = false,
                        zoomGesturesEnabled = false
                    )
                )
            }
            // Observing and controlling the camera's state can be done with a CameraPositionState
            val cameraPositionState = rememberCameraPositionState {
                position = defaultCameraPosition
            }

            Box(Modifier.fillMaxSize()) {
                GoogleMap(
                    modifier = Modifier.matchParentSize(),
                    cameraPositionState = cameraPositionState,
                    onMapLoaded = {
                        isMapLoaded = true
                    },
                    uiSettings = uiSettings,
                )

                if (!isMapLoaded) {
                    AnimatedVisibility(
                        modifier = Modifier
                            .matchParentSize(),
                        visible = !isMapLoaded,
                        enter = EnterTransition.None,
                        exit = fadeOut()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .background(MaterialTheme.colors.background)
                                .wrapContentSize()
                        )
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MapButton(
                        "This is a custom location button",
                        onClick = {
                            Toast.makeText(
                                this@CustomControlsActivity,
                                "Click on my location",
                                Toast.LENGTH_SHORT
                            ).show()
                        })
                    MapButton(
                        "+",
                        onClick = {
                            coroutineScope.launch {
                                cameraPositionState.animate(CameraUpdateFactory.zoomIn())
                            }
                        })
                    MapButton(
                        "-",
                        onClick = {
                            coroutineScope.launch {
                                cameraPositionState.animate(CameraUpdateFactory.zoomOut())
                            }
                        })
                }
            }
        }
    }

    @Composable
    private fun MapButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
        Button(
            modifier = modifier.padding(4.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.onPrimary,
                contentColor = MaterialTheme.colors.primary
            ),
            onClick = onClick
        ) {
            Text(text = text, style = MaterialTheme.typography.body1)
        }
    }


}
