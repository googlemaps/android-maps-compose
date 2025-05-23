// Copyright 2022 Google LLC
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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.theme.MapsComposeSampleTheme
import com.google.maps.android.compose.widgets.DarkGray
import com.google.maps.android.compose.widgets.DisappearingScaleBar
import com.google.maps.android.compose.widgets.ScaleBar

class ScaleBarActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isMapLoaded by remember { mutableStateOf(false) }

            // To control and observe the map camera
            val cameraPositionState = rememberCameraPositionState {
                position = defaultCameraPosition
            }

            val scaleBackground = MaterialTheme.colors.background.copy(alpha = 0.4f)
            val scaleBorderStroke = BorderStroke(width = 1.dp, DarkGray.copy(alpha = 0.2f))

            Box(
                modifier = Modifier.fillMaxSize()
                    .systemBarsPadding(),
            ) {
                GoogleMap(
                    modifier = Modifier.matchParentSize(),
                    cameraPositionState = cameraPositionState,
                    onMapLoaded = {
                        isMapLoaded = true
                    }
                )

                Box(
                    modifier = Modifier
                        .padding(top = 5.dp, start = 5.dp)
                        .align(Alignment.TopStart)
                        .background(
                            scaleBackground,
                            shape = MaterialTheme.shapes.medium
                        )
                        .border(
                            scaleBorderStroke,
                            shape = MaterialTheme.shapes.medium
                        ),
                ) {
                    DisappearingScaleBar(
                        modifier = Modifier.padding(end = 4.dp),
                        cameraPositionState = cameraPositionState
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(top = 5.dp, end = 5.dp)
                        .align(Alignment.TopEnd)
                        .background(
                            scaleBackground,
                            shape = MaterialTheme.shapes.medium,
                        )
                        .border(
                            scaleBorderStroke,
                            shape = MaterialTheme.shapes.medium
                        ),
                    ) {
                    ScaleBar(
                        modifier = Modifier.padding(end = 4.dp),
                        cameraPositionState = cameraPositionState
                    )

                }
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
            }
        }
    }
}

@Preview
@Composable
fun PreviewScaleBar() {
    val cameraPositionState = remember {
        CameraPositionState(
            position = CameraPosition(
                LatLng(48.137154, 11.576124), // Example coordinates: Munich, Germany
                12f,
                0f,
                0f
            )
        )
    }

    MapsComposeSampleTheme {
        ScaleBar(
            modifier = Modifier.padding(end = 4.dp),
            cameraPositionState = cameraPositionState
        )
    }
}