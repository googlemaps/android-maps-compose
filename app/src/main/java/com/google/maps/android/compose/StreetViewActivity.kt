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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.Button
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextField
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
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.StreetViewPanoramaOptions
import com.google.maps.android.compose.streetview.StreetView
import com.google.maps.android.compose.streetview.rememberStreetViewCameraPositionState
import com.google.maps.android.ktx.MapsExperimentalFeature
import kotlinx.coroutines.launch

class StreetViewActivity : ComponentActivity() {

    private val TAG = StreetViewActivity::class.java.simpleName

    @OptIn(MapsExperimentalFeature::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isPanningEnabled by remember { mutableStateOf(false) }
            var isZoomEnabled by remember { mutableStateOf(false) }
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
            }
            Box(Modifier.fillMaxSize(), Alignment.BottomStart) {
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