// Copyright 2023 Google LLC
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

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.widgets.ScaleBar
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch

class ScaleBarTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var cameraPositionState: CameraPositionState

    private fun initScaleBar(initialZoom: Float, initialPosition: LatLng) {
        check(hasValidApiKey) { "Maps API key not specified" }

        cameraPositionState = CameraPositionState(
            position = CameraPosition.fromLatLngZoom(initialPosition, initialZoom)
        )

        composeTestRule.setContent {
            ScaleBar(cameraPositionState = cameraPositionState)
        }
    }

    @Test
    fun testScaleBarInitialState() {
        val initialZoom = 15f
        val initialPosition = LatLng(37.7749, -122.4194) // San Francisco
        initScaleBar(initialZoom, initialPosition)

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(
            text = "ft",
            substring = true,
            ignoreCase = false
        ).assertExists()
        composeTestRule.onNodeWithText(
            text = "m",
            substring = true,
            ignoreCase = false
        ).assertExists()
    }
}