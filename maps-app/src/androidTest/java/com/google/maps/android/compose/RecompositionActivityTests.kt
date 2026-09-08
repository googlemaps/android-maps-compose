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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.android.gms.maps.model.CameraPosition
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class RecompositionActivityTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testChangeLocationButton_MovesCameraToNewLocation() {
        check(hasValidApiKey) { "Maps API key not specified" }
        val initialPosition = singapore
        val cameraPositionState = CameraPositionState(
            position = CameraPosition.fromLatLngZoom(initialPosition, 11f)
        )

        composeTestRule.setContent {
            RecompositionActivity().GoogleMapView(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            )
        }

        // Verify initial camera target is centered at Singapore
        initialPosition.assertEquals(cameraPositionState.position.target)

        // Click the "Change Location" button
        composeTestRule.onNodeWithText("Change Location")
            .assertIsDisplayed()
            .performClick()

        // Wait until the camera target moves away from the initial position
        composeTestRule.waitUntil(timeout5) {
            cameraPositionState.position.target != initialPosition
        }

        // Wait until the camera animation has completed
        composeTestRule.waitUntil(timeout5) {
            !cameraPositionState.isMoving
        }

        // Verify that the camera has actually moved to a new location
        assertNotEquals(initialPosition, cameraPositionState.position.target)
    }
}
