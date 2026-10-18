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

import android.graphics.Bitmap
import android.graphics.Point
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.widgets.ScaleBar
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.Locale

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

    private fun assertFractionalLabels(dark: Boolean) {
        val originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            val projection = mockk<Projection>()
            every { projection.fromScreenLocation(any()) } answers {
                // At the equator this spans about 2,224 m, or 1,976 m for the 8/9-width bar.
                if (firstArg<Point>().x == 0) LatLng(0.0, 0.0) else LatLng(0.0, 0.02)
            }
            val camera = mockk<CameraPositionState>()
            every { camera.position } returns CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 12f)
            every { camera.projection } returns projection
            val background = if (dark) Color.Black else Color.White
            val foreground = if (dark) Color.White else Color.Black
            composeTestRule.setContent {
                MaterialTheme {
                    Box(Modifier.testTag("scale-card").background(background).padding(16.dp)) {
                        ScaleBar(
                            cameraPositionState = camera,
                            textColor = foreground,
                            lineColor = foreground,
                            shadowColor = background,
                        )
                    }
                }
            }
            composeTestRule.onNodeWithText("1.98 km").assertIsDisplayed()
            composeTestRule.onNodeWithText("1.23 mi").assertIsDisplayed()

            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val image = composeTestRule.onNodeWithTag("scale-card").captureToImage().asAndroidBitmap()
            val output = File(context.getExternalFilesDir(null), "scale-bar-${if (dark) "dark" else "light"}.png")
            output.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}
