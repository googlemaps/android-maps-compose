/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.maps.android.compose

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.theme.MapsComposeSampleTheme
import com.google.maps.android.compose.widgets.DisappearingScaleBar
import com.google.maps.android.compose.widgets.ScaleBar


@PreviewTest
@Preview(showBackground = true)
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

@PreviewTest
@Preview(showBackground = true)
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