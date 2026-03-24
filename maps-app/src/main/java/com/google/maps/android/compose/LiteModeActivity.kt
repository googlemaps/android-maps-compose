package com.google.maps.android.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.theme.MapsComposeSampleTheme
import kotlinx.coroutines.launch

class LiteModeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MapsComposeSampleTheme {
                val singapore = remember { LatLng(1.35, 103.87) }
                val tokyo = remember { LatLng(1.35, 103.87) } // wait let me fix coords below
                val coroutineScope = rememberCoroutineScope()
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(singapore, 11f)
                }
                val mapProperties by remember {
                    mutableStateOf(MapProperties(mapType = MapType.NORMAL))
                }

                Column(
                    modifier = Modifier.fillMaxSize().systemBarsPadding()
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                // This would previously hang indefinitely in Lite Mode!
                                // Now it falls back to instantaneous movement and completes the coroutine.
                                val newTarget = if (cameraPositionState.position.target == singapore) {
                                    LatLng(35.6895, 139.6917) // Tokyo
                                } else {
                                    singapore
                                }
                                cameraPositionState.animate(CameraUpdateFactory.newLatLng(newTarget))
                            }
                        },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("Animate Camera (Tests Fix)")
                    }

                    Box(
                        modifier = Modifier.weight(1f),
                    ) {
                        GoogleMap(
                            modifier = Modifier.matchParentSize(),
                            googleMapOptionsFactory = { GoogleMapOptions().liteMode(true) },
                            cameraPositionState = cameraPositionState,
                            properties = mapProperties,
                        )
                    }
                }
            }
        }
    }
}
