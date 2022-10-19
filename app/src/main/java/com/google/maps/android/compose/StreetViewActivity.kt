package com.google.maps.android.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Switch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.LineHeightStyle
import com.google.android.gms.maps.StreetViewPanoramaOptions
import com.google.maps.android.compose.streetview.StreetView

class StreetViewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isZoomEnabled by remember { mutableStateOf(false) }
            Box(Modifier.fillMaxSize(), Alignment.BottomStart) {
                StreetView(Modifier.matchParentSize(), streetViewPanoramaOptionsFactory = {
                    StreetViewPanoramaOptions().position(singapore)
                }, isZoomGesturesEnabled = isZoomEnabled)
                Row(Modifier.fillMaxWidth()) {
                    Switch(checked = isZoomEnabled, onCheckedChange = {
                        isZoomEnabled = it
                    })
                }
            }
        }
    }
}