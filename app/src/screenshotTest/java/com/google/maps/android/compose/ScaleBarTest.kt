package com.google.maps.android.compose

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.theme.MapsComposeSampleTheme
import com.google.maps.android.compose.widgets.DisappearingScaleBar
import com.google.maps.android.compose.widgets.ScaleBar


class ScaleBarTest() {
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

    @Preview
    @Composable
    fun PreviewDisappearingScaleBar() {
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
            DisappearingScaleBar(
                modifier = Modifier.padding(end = 4.dp),
                cameraPositionState = cameraPositionState
            )
        }
    }
}