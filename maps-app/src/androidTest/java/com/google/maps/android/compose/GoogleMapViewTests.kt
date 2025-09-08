// Copyright 2024 Google LLC
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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
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
    private var mapColorScheme = ComposeMapColorScheme.FOLLOW_SYSTEM

    private fun initMap(content: @Composable () -> Unit = {}) {
        check(hasValidApiKey) { "Maps API key not specified" }
        val countDownLatch = CountDownLatch(1)
        composeTestRule.setContent {
            GoogleMapView(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapLoaded = {
                    countDownLatch.countDown()
                },
                mapColorScheme = mapColorScheme
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
    fun testRightInitialColorScheme() {
        initMap()
        mapColorScheme.assertEquals(ComposeMapColorScheme.FOLLOW_SYSTEM)
    }

    @Test
    fun testRightColorSchemeAfterChangingIt() {
        mapColorScheme = ComposeMapColorScheme.DARK
        initMap()
        mapColorScheme.assertEquals(ComposeMapColorScheme.DARK)
    }

    @Test
    fun testCameraReportsMoving() {
        initMap()
        assertEquals(CameraMoveStartedReason.NO_MOVEMENT_YET, cameraPositionState.cameraMoveStartedReason)
        zoom(shouldAnimate = true, zoomIn = true) {
            composeTestRule.waitUntil(timeout2) {
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
            composeTestRule.waitUntil(timeout2) {
                cameraPositionState.isMoving
            }
            composeTestRule.waitUntil(timeout5) {
                !cameraPositionState.isMoving
            }
            assertFalse(cameraPositionState.isMoving)
        }
    }

    @Test
    fun testCameraZoomInAnimation() {
        initMap()
        zoom(shouldAnimate = true, zoomIn = true) {
            composeTestRule.waitUntil(timeout2) {
                cameraPositionState.isMoving
            }
            composeTestRule.waitUntil(timeout3) {
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
            composeTestRule.waitUntil(timeout2) {
                cameraPositionState.isMoving
            }
            composeTestRule.waitUntil(timeout3) {
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
            composeTestRule.waitUntil(timeout2) {
                cameraPositionState.isMoving
            }
            composeTestRule.waitUntil(timeout3) {
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
            composeTestRule.waitUntil(timeout2) {
                cameraPositionState.isMoving
            }
            composeTestRule.waitUntil(timeout3) {
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
            val markerState = rememberUpdatedMarkerState()
            Marker(
                state = markerState
            )
            Marker(
                state = markerState
            )
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testMarkerStateInsideMarkerComposableCannotBeReused() {
        initMap {
            val markerState = rememberUpdatedMarkerState()
            MarkerComposable(
                keys = arrayOf("marker1"),
                state = markerState,
            ) {
                Box {
                    Text(text = "marker1")
                }
            }
            MarkerComposable(
                keys = arrayOf("marker2"),
                state = markerState,
            ) {
                Box {
                    Text(text = "marker2")
                }
            }
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testMarkerStateInsideMarkerInfoWindowComposableCannotBeReused() {
        initMap {
            val markerState = rememberUpdatedMarkerState()
            MarkerInfoWindowComposable(
                keys = arrayOf("marker1"),
                state = markerState,
            ) {
                Box {
                    Text(text = "marker1")
                }
            }
            MarkerInfoWindowComposable(
                keys = arrayOf("marker2"),
                state = markerState,
            ) {
                Box {
                    Text(text = "marker2")
                }
            }
        }
    }

    @Test
    fun testCameraPositionStateMapClears() {
        initMap()
        composeTestRule.onNodeWithTag("toggleMapVisibility")
            .performClick()
            .performClick()
    }

    @Test
    fun testRememberUpdatedMarkerStateBeUpdate() {
        val testPoint0 = LatLng(0.0,0.0)
        val testPoint1 = LatLng(37.6281576,-122.4264549)
        val testPoint2 = LatLng(37.500012, 127.0364185)

        val positionState = mutableStateOf(testPoint0)
        lateinit var markerState: MarkerState

        initMap {
            markerState = rememberUpdatedMarkerState(position = positionState.value)
        }

        assertEquals(testPoint0, markerState.position)

        positionState.value = testPoint1
        composeTestRule.waitForIdle()
        assertEquals(testPoint1, markerState.position)

        positionState.value = testPoint2
        composeTestRule.waitForIdle()
        assertEquals(testPoint2, markerState.position)
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
