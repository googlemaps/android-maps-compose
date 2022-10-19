package com.google.maps.android.compose

import android.os.Bundle
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
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.StreetViewPanoramaOptions
import com.google.maps.android.compose.streetview.StreetView

class StreetViewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isPanningEnabled by remember { mutableStateOf(false) }
            var isZoomEnabled by remember { mutableStateOf(false) }
            Box(Modifier.fillMaxSize(), Alignment.BottomStart) {
                StreetView(
                    Modifier.matchParentSize(),
                    streetViewPanoramaOptionsFactory = {
                        StreetViewPanoramaOptions().position(singapore)
                    },
                    isPanningGesturesEnabled = isPanningEnabled,
                    isZoomGesturesEnabled = isZoomEnabled
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(8.dp)
                ) {
                    StreetViewSwitch(title = "Panning", checked =isPanningEnabled) {
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