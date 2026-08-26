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

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapColorScheme
import com.google.common.truth.Truth.assertThat
import com.google.maps.android.compose.LatLngSubject.Companion.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs

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

        val appContext: Context = InstrumentationRegistry.getInstrumentation().targetContext


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
        val mapLoaded = countDownLatch.await(MAP_LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        assertThat(mapLoaded).isTrue()
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
        assertThat(cameraPositionState.position.target).isEqualTo(startingPosition)
    }

    @Test
    fun testRightInitialColorScheme() {
        var capturedOptions: GoogleMapOptions? = null
        composeTestRule.setContent {
            GoogleMap(
                mapColorScheme = ComposeMapColorScheme.FOLLOW_SYSTEM,
                mapViewFactory = { context, options ->
                    capturedOptions = options
                    MapView(context, options)
                }
            )
        }
        assertThat(capturedOptions?.mapColorScheme).isEqualTo(MapColorScheme.FOLLOW_SYSTEM)
    }

    @Test
    fun testRightColorSchemeAfterChangingIt() {
        var capturedOptions: GoogleMapOptions? = null
        composeTestRule.setContent {
            GoogleMap(
                mapColorScheme = ComposeMapColorScheme.DARK,
                mapViewFactory = { context, options ->
                    capturedOptions = options
                    MapView(context, options)
                }
            )
        }
        assertThat(capturedOptions?.mapColorScheme).isEqualTo(MapColorScheme.DARK)
    }

    @Test
    fun testColorSchemeInOptionsNotOverwritten() {
        var capturedOptions: GoogleMapOptions? = null
        composeTestRule.setContent {
            GoogleMap(
                googleMapOptionsFactory = {
                    GoogleMapOptions().mapColorScheme(MapColorScheme.DARK)
                },
                mapColorScheme = ComposeMapColorScheme.FOLLOW_SYSTEM,
                mapViewFactory = { context, options ->
                    capturedOptions = options
                    MapView(context, options)
                }
            )
        }
        // When googleMapOptionsFactory explicitly sets a color scheme (e.g. DARK), it should not be overwritten by mapColorScheme
        assertThat(capturedOptions?.mapColorScheme).isEqualTo(MapColorScheme.DARK)
    }

    @Test
    fun testCameraReportsMoving() {
        initMap()
        assertThat(cameraPositionState.cameraMoveStartedReason).isEqualTo(CameraMoveStartedReason.NO_MOVEMENT_YET)
        zoom(shouldAnimate = true, zoomIn = true) {
            composeTestRule.waitUntil(timeout2) {
                cameraPositionState.isMoving
            }
            assertThat(cameraPositionState.isMoving).isTrue()
            assertThat(cameraPositionState.cameraMoveStartedReason).isEqualTo(CameraMoveStartedReason.DEVELOPER_ANIMATION)
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
            assertThat(cameraPositionState.isMoving).isFalse()
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
            assertThat(cameraPositionState.position.zoom).isWithin(assertRoundingError.toFloat()).of(startingZoom + 1f)
        }
    }

    @Test
    fun testCameraZoomIn() {
        initMap()
        zoom(shouldAnimate = false, zoomIn = true) {
            val expectedZoom = startingZoom + 1f
            // Non-animated camera updates (CameraPositionState.move) execute synchronously via
            // GoogleMap.moveCamera(). The Maps SDK fires OnCameraMoveStartedListener (setting
            // isMoving = true) and OnCameraIdleListener (setting isMoving = false) in rapid succession.
            //
            // Because Compose test polling samples state across frames, polling for the transient
            // `isMoving = true` state is flaky. Instead:
            // 1. We first wait until the zoom transition has actually occurred.
            // 2. We then wait for the camera to settle completely (!isMoving).
            composeTestRule.waitUntil(timeout3) {
                abs(cameraPositionState.position.zoom - expectedZoom) <= assertRoundingError
            }
            assertThat(cameraPositionState.position.zoom).isWithin(assertRoundingError.toFloat()).of(expectedZoom)

            composeTestRule.waitUntil(timeout3) {
                !cameraPositionState.isMoving
            }
            assertThat(cameraPositionState.isMoving).isFalse()
        }
    }

    @Test
    fun testCameraZoomOut() {
        initMap()
        zoom(shouldAnimate = false, zoomIn = false) {
            val expectedZoom = startingZoom - 1f
            // Non-animated camera updates (CameraPositionState.move) execute synchronously via
            // GoogleMap.moveCamera(). The Maps SDK fires OnCameraMoveStartedListener (setting
            // isMoving = true) and OnCameraIdleListener (setting isMoving = false) in rapid succession.
            //
            // Because Compose test polling samples state across frames, polling for the transient
            // `isMoving = true` state is flaky. Instead:
            // 1. We first wait until the zoom transition has actually occurred.
            // 2. We then wait for the camera to settle completely (!isMoving).
            composeTestRule.waitUntil(timeout3) {
                abs(cameraPositionState.position.zoom - expectedZoom) <= assertRoundingError
            }
            assertThat(cameraPositionState.position.zoom).isWithin(assertRoundingError.toFloat()).of(expectedZoom)

            composeTestRule.waitUntil(timeout3) {
                !cameraPositionState.isMoving
            }
            assertThat(cameraPositionState.isMoving).isFalse()
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
            assertThat(cameraPositionState.position.zoom).isWithin(assertRoundingError.toFloat()).of(startingZoom - 1f)
        }
    }

    @Test
    fun testLatLngInVisibleRegion() {
        initMap()
        composeTestRule.runOnUiThread {
            val projection = cameraPositionState.projection
            assertThat(projection).isNotNull()
            assertThat(
                projection!!.visibleRegion.latLngBounds.contains(startingPosition)
            ).isTrue()
        }
    }

    @Test
    fun testLatLngNotInVisibleRegion() {
        initMap()
        composeTestRule.runOnUiThread {
            val projection = cameraPositionState.projection
            assertThat(projection).isNotNull()
            val latLng = LatLng(23.4, 25.6)
            assertThat(
                projection!!.visibleRegion.latLngBounds.contains(latLng)
            ).isFalse()
        }
    }

    @Test
    fun testCameraZoomLevels() {
        assertThat(cameraPositionState.minZoomLevel).isNull()
        assertThat(cameraPositionState.maxZoomLevel).isNull()

        initMap()

        composeTestRule.runOnUiThread {
            val minZoomLevel = cameraPositionState.minZoomLevel
            val maxZoomLevel = cameraPositionState.maxZoomLevel
            assertThat(minZoomLevel).isNotNull()
            assertThat(maxZoomLevel).isNotNull()
            assertThat(minZoomLevel!!).isAtMost(maxZoomLevel!!)
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

    @Test
    fun testAdvancedMarkerInfoWindowContentDoesNotCrash() {
        // Regression test for https://github.com/googlemaps/android-maps-compose/issues/822:
        // AdvancedMarker didn't expose infoContent/infoWindow customization, even though the
        // underlying implementation already supported it. Custom info window content is
        // rendered to a bitmap by ComposeInfoWindowAdapter (not present in the Compose
        // semantics tree), so this asserts the composable wires up and shows without crashing
        // rather than asserting on-screen content.
        lateinit var markerState: MarkerState

        initMap {
            markerState = rememberUpdatedMarkerState(position = startingPosition)
            AdvancedMarkerInfoWindowContent(state = markerState) {
                Text(text = "custom advanced marker info window")
            }
        }

        composeTestRule.runOnUiThread {
            markerState.showInfoWindow()
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun testAdvancedMarkerInfoWindowDoesNotCrash() {
        // Regression test for https://github.com/googlemaps/android-maps-compose/issues/822:
        // see testAdvancedMarkerInfoWindowContentDoesNotCrash for why this doesn't assert
        // on-screen content.
        lateinit var markerState: MarkerState

        initMap {
            markerState = rememberUpdatedMarkerState(position = startingPosition)
            AdvancedMarkerInfoWindow(state = markerState) {
                Text(text = "custom advanced marker whole info window")
            }
        }

        composeTestRule.runOnUiThread {
            markerState.showInfoWindow()
        }
        composeTestRule.waitForIdle()
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

        assertThat(markerState.position).isEqualTo(testPoint0)

        positionState.value = testPoint1
        composeTestRule.waitForIdle()
        assertThat(markerState.position).isEqualTo(testPoint1)

        positionState.value = testPoint2
        composeTestRule.waitForIdle()
        assertThat(markerState.position).isEqualTo(testPoint2)
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
