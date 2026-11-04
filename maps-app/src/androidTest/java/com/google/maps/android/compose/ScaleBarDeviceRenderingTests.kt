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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.google.maps.android.compose.widgets.ScaleBar
import org.junit.Rule
import org.junit.Test

class ScaleBarDeviceRenderingTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun unboundCameraRendersOnLightBackground() {
        assertInitialLabels(dark = false)
    }

    @Test
    fun unboundCameraRendersOnDarkBackground() {
        assertInitialLabels(dark = true)
    }

    private fun assertInitialLabels(dark: Boolean) {
        val camera = CameraPositionState()
        val background = if (dark) Color.Black else Color.White
        val foreground = if (dark) Color.White else Color.Black
        composeTestRule.setContent {
            MaterialTheme {
                Box(Modifier.background(background).padding(16.dp)) {
                    ScaleBar(
                        cameraPositionState = camera,
                        textColor = foreground,
                        lineColor = foreground,
                        shadowColor = background,
                    )
                }
            }
        }
        composeTestRule.onNodeWithText("0 m").assertIsDisplayed()
        composeTestRule.onNodeWithText("0 ft").assertIsDisplayed()
    }
}
