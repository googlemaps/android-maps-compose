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

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.android.gms.maps.StreetViewPanoramaOptions
import com.google.android.gms.maps.model.StreetViewPanoramaOrientation
import com.google.maps.android.compose.streetview.StreetView
import com.google.maps.android.compose.streetview.StreetViewCameraPositionState
import com.google.maps.android.ktx.MapsExperimentalFeature
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class StreetViewTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var cameraPositionState: StreetViewCameraPositionState
    private val initialLatLng = singapore

    @Before
    fun setUp() {
        cameraPositionState = StreetViewCameraPositionState()
    }

    @OptIn(MapsExperimentalFeature::class)
    private fun initStreetView(onClick: (StreetViewPanoramaOrientation) -> Unit = {}) {
        composeTestRule.setContent {
            StreetView(
                Modifier.semantics { contentDescription = "StreetView" },
                cameraPositionState = cameraPositionState,
                streetViewPanoramaOptionsFactory = {
                    StreetViewPanoramaOptions()
                        .position(initialLatLng)
                },
                onClick = onClick
            )
        }
        composeTestRule.waitUntil(timeout5) {
            cameraPositionState.location.position.latitude != 0.0 &&
                cameraPositionState.location.position.longitude != 0.0
        }
    }

    @Test
    fun testStartingStreetViewPosition() {
        initStreetView()
        initialLatLng.assertEquals(cameraPositionState.location.position)
    }
}