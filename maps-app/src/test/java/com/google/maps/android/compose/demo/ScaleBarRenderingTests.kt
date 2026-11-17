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

package com.google.maps.android.compose.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.widgets.ScaleBar
import com.google.maps.android.robolectric.annotation.EnableMapsShadows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29, 35])
@EnableMapsShadows
class ScaleBarRenderingTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun fractionalLabelsRenderOnLightBackground() {
        assertFractionalLabels(dark = false)
    }

    @Test
    fun fractionalLabelsRenderOnDarkBackground() {
        assertFractionalLabels(dark = true)
    }

    @Test
    fun largeLabelsRenderOnLightBackground() {
        assertFractionalLabels(dark = false, large = true)
    }

    @Test
    fun largeLabelsRenderOnDarkBackground() {
        assertFractionalLabels(dark = true, large = true)
    }

    private fun assertFractionalLabels(dark: Boolean, large: Boolean = false) {
        // At the equator on a 65 dp bar, zoom 12.15f spans ~0.02° (~1,976 m for the 8/9-width bar)
        // and zoom 2.1922927f spans 20° (1,976,800 m -> "1977 km" / "1228 mi" for the 8/9-width bar).
        val camera = CameraPositionState(
            position = CameraPosition.fromLatLngZoom(
                LatLng(0.0, 0.0),
                if (large) 2.1922927f else 12.15f,
            ),
        )
        val background = if (dark) Color.Black else Color.White
        val foreground = if (dark) Color.White else Color.Black
        composeTestRule.setContent {
            MaterialTheme {
                Box(Modifier.size(200.dp).background(background).padding(16.dp)) {
                    GoogleMap(
                        modifier = Modifier.matchParentSize(),
                        cameraPositionState = camera,
                    )
                    ScaleBar(
                        cameraPositionState = camera,
                        textColor = foreground,
                        lineColor = foreground,
                        shadowColor = background,
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(if (large) "1977 km" else "2 km").assertIsDisplayed()
        composeTestRule.onNodeWithText(if (large) "1228 mi" else "1.2 mi").assertIsDisplayed()
    }
}
