// Copyright 2026 Google LLC
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
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.theme.MapsComposeSampleTheme
import androidx.core.graphics.createBitmap

class GroundOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MapsComposeSampleTheme {
                GroundOverlayScreen()
            }
        }
    }
}

@Composable
fun GroundOverlayScreen() {
    val singapore = LatLng(1.3588227, 103.8742114)
    val cameraPositionState = rememberCameraPositionState {
        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(singapore, 12f)
    }

    var isVisible by remember { mutableStateOf(true) }
    var transparency by remember { mutableFloatStateOf(0f) }
    var bearing by remember { mutableFloatStateOf(0f) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val imageDescriptor = remember {
        val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
        val bitmap = createBitmap(drawable!!.intrinsicWidth, drawable.intrinsicHeight)
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            // Stable GroundOverlay (using remembered state)
            GroundOverlay(
                position = GroundOverlayPosition.create(
                    LatLngBounds(
                        LatLng(1.35, 103.86),
                        LatLng(1.37, 103.88)
                    )
                ),
                image = imageDescriptor,
                visible = isVisible,
                transparency = transparency,
                bearing = bearing
            )

            // Stress-test GroundOverlay: Re-creating position every recomposition
            // This would cause a crash/rendering loop if GroundOverlayPosition was not a data class
            GroundOverlay(
                position = GroundOverlayPosition.create(
                    LatLngBounds(
                        LatLng(1.36, 103.89),
                        LatLng(1.38, 103.91)
                    )
                ),
                image = imageDescriptor,
                transparency = 0.5f,
                zIndex = 1f
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            GroundOverlayControls(
                isVisible = isVisible,
                onVisibilityChange = { isVisible = it },
                transparency = transparency,
                onTransparencyChange = { transparency = it },
                bearing = bearing,
                onBearingChange = { bearing = it }
            )
        }
    }
}

@Composable
fun GroundOverlayControls(
    isVisible: Boolean,
    onVisibilityChange: (Boolean) -> Unit,
    transparency: Float,
    onTransparencyChange: (Float) -> Unit,
    bearing: Float,
    onBearingChange: (Float) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Visible", modifier = Modifier.weight(1f))
                Switch(checked = isVisible, onCheckedChange = onVisibilityChange)
            }
            Text(text = "Transparency: ${String.format(Locale.getDefault(), "%.2f", transparency)}")
            Slider(
                value = transparency,
                onValueChange = onTransparencyChange,
                valueRange = 0f..1f
            )
            Text(text = "Bearing: ${bearing.toInt()}°")
            Slider(
                value = bearing,
                onValueChange = onBearingChange,
                valueRange = 0f..360f
            )
        }
    }
}
