// Copyright 2022 Google LLC
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class GoogleMapViewTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val startingZoom = 10f
    private val startingPosition = LatLng(1.23, 4.56)
    private lateinit var cameraPositionState: CameraPositionState

    private fun initMap(content: @Composable () -> Unit = {}) {
        val countDownLatch = CountDownLatch(1)
        composeTestRule.setContent {
            GoogleMapView(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapLoaded = {
                    countDownLatch.countDown()
                }
            ) {
                content.invoke()
            }
        }
        val mapLoaded = countDownLatch.await(30, TimeUnit.SECONDS)
        assertTrue("Map loaded", mapLoaded)
    }

    @Before
    fun setUp() {
        cameraPositionState = CameraPositionState(
            position = CameraPosition.fromLatLngZoom(
                startingPosition,
                startingZoom
            )
        )
    }

    @Test
    fun testStartingCameraPosition() {
        initMap()
        startingPosition.assertEquals(cameraPositionState.position.target)
    }

    @Test
    fun testCameraReportsMoving() {
        initMap()
        assertEquals(CameraMoveStartedReason.NO_MOVEMENT_YET, cameraPositionState.cameraMoveStartedReason)
        zoom(shouldAnimate = true, zoomIn = true) {
            composeTestRule.waitUntil(1000) {
                cameraPositionState.isMoving
            }
            assertTrue(cameraPositionState.isMoving)
            assertEquals(CameraMoveStartedReason.DEVELOPER_ANIMATION, cameraPositionState.cameraMoveStartedReason)
        }
    }

    @Test
    fun testCameraReportsNotMoving() {
        initMap()
        zoom(shouldAnimate = true, zoomIn = true) {
            composeTestRule.waitUntil(1000) {
                cameraPositionState.isMoving
            }
            composeTestRule.waitUntil(5000) {
                !cameraPositionState.isMoving
            }
            assertFalse(cameraPositionState.isMoving)
        }
    }

    @Test
    fun testCameraZoomInAnimation() {
        initMap()
        zoom(shouldAnimate = true, zoomIn = true) {
            composeTestRule.waitUntil(1000) {
                cameraPositionState.isMoving
            }
            composeTestRule.waitUntil(3000) {
                !cameraPositionState.isMoving
            }
            assertEquals(
                startingZoom + 1f,
                cameraPositionState.position.zoom,
                assertRoundingError.toFloat()
            )
        }
    }

    @Test
    fun testCameraZoomIn() {
        initMap()
        zoom(shouldAnimate = false, zoomIn = true) {
            composeTestRule.waitUntil(1000) {
                cameraPositionState.isMoving
            }
            composeTestRule.waitUntil(3000) {
                !cameraPositionState.isMoving
            }
            assertEquals(
                startingZoom + 1f,
                cameraPositionState.position.zoom,
                assertRoundingError.toFloat()
            )
        }
    }

    @Test
    fun testCameraZoomOut() {
        initMap()
        zoom(shouldAnimate = false, zoomIn = false) {
            composeTestRule.waitUntil(1000) {
                cameraPositionState.isMoving
            }
            composeTestRule.waitUntil(3000) {
                !cameraPositionState.isMoving
            }
            assertEquals(
                startingZoom - 1f,
                cameraPositionState.position.zoom,
                assertRoundingError.toFloat()
            )
        }
    }

    @Test
    fun testCameraZoomOutAnimation() {
        initMap()
        zoom(shouldAnimate = true, zoomIn = false) {
            composeTestRule.waitUntil(1000) {
                cameraPositionState.isMoving
            }
            composeTestRule.waitUntil(3000) {
                !cameraPositionState.isMoving
            }
            assertEquals(
                startingZoom - 1f,
                cameraPositionState.position.zoom,
                assertRoundingError.toFloat()
            )
        }
    }

    @Test
    fun testLatLngInVisibleRegion() {
        initMap()
        composeTestRule.runOnUiThread {
            val projection = cameraPositionState.projection
            assertNotNull(projection)
            assertTrue(
                projection!!.visibleRegion.latLngBounds.contains(startingPosition)
            )
        }
    }

    @Test
    fun testLatLngNotInVisibleRegion() {
        initMap()
        composeTestRule.runOnUiThread {
            val projection = cameraPositionState.projection
            assertNotNull(projection)
            val latLng = LatLng(23.4, 25.6)
            assertFalse(
                projection!!.visibleRegion.latLngBounds.contains(latLng)
            )
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testMarkerStateCannotBeReused() {
        initMap {
            val markerState = rememberMarkerState()
            Marker(
                state = markerState
            )
            Marker(
                state = markerState
            )
        }
    }

    @Test
    fun testCameraPositionStateMapClears() {
        initMap()
        composeTestRule.onNodeWithTag("toggleMapVisibility")
            .performClick()
            .performClick()
    }

    private fun zoom(
        shouldAnimate: Boolean,
        zoomIn: Boolean,
        assertionBlock: () -> Unit
    ) {
        if (!shouldAnimate) {
            composeTestRule.onNodeWithTag("cameraAnimations")
                .assertIsDisplayed()
                .performClick()
        }
        composeTestRule.onNodeWithText(if (zoomIn) "+" else "-")
            .assertIsDisplayed()
            .performClick()

        assertionBlock()
    }
}