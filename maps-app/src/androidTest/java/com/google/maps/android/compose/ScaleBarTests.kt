// Copyright 2025 Google LLC
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

import android.content.pm.ActivityInfo
import android.graphics.Point
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.common.truth.Truth.assertThat
import com.google.maps.android.ImperialUnitsConverter
import com.google.maps.android.MetricUnitsConverter
import com.google.maps.android.NumberFormatSymbols
import com.google.maps.android.compose.widgets.ScaleBar
import com.google.maps.android.meters
import com.google.maps.android.miles
import com.google.maps.android.sphericalDistance
import com.google.maps.android.toDistanceString
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs

private val scaleBarImperialConverter = ImperialUnitsConverter(milesThreshold = 1.miles)

class ScaleBarTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var cameraPositionState: CameraPositionState
    private lateinit var density: Density

    @After
    fun tearDown() {
        runCatching {
            composeTestRule.activityRule.scenario.onActivity { activity ->
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    private fun initScaleBar(initialZoom: Float, initialPosition: LatLng, measureAtCenter: Boolean = true) {
        assumeValidApiKey()

        val countDownLatch = CountDownLatch(1)

        cameraPositionState = CameraPositionState(
            position = CameraPosition.fromLatLngZoom(initialPosition, initialZoom),
        )

        composeTestRule.setContent {
            density = LocalDensity.current
            Box {
                GoogleMap(
                    cameraPositionState = cameraPositionState,
                    onMapLoaded = {
                        countDownLatch.countDown()
                    },
                )
                ScaleBar(
                    cameraPositionState = cameraPositionState,
                    measureAtCenter = measureAtCenter,
                )
            }
        }
        val mapLoaded = countDownLatch.await(MAP_LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        assertTrue(mapLoaded)
    }

    @Test
    fun testScaleBarInitialState() {
        assertScaleBarLabels(initialZoom = 15f)
    }

    @Test
    fun testScaleBarFractionalKilometers() {
        assertScaleBarLabels(initialZoom = 12f)
    }

    @Test
    fun testScaleBarFractionalMiles() {
        assertScaleBarLabels(initialZoom = 10f)
    }

    @Test
    fun testScaleBarUpdatesWhenLatitudeChangesAtSameZoom() {
        val equator = LatLng(0.0, 0.0)
        val iceland = LatLng(64.9631, -19.0208)
        initScaleBar(initialZoom = 10f, initialPosition = equator)
        composeTestRule.waitForIdle()

        val (_, equatorMetric, equatorMeters) = computeExpectedLabels()
        composeTestRule.onNodeWithText(equatorMetric).assertExists()

        composeTestRule.runOnIdle {
            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(iceland, 10f))
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            !cameraPositionState.isMoving &&
                abs(cameraPositionState.position.target.latitude - iceland.latitude) < 0.1
        }
        composeTestRule.waitForIdle()

        val (icelandImperial, icelandMetric, icelandMeters) = computeExpectedLabels()
        assertThat(icelandMeters).isLessThan(equatorMeters / 2)
        composeTestRule.onNodeWithText(icelandImperial).assertExists()
        composeTestRule.onNodeWithText(icelandMetric).assertExists()
    }

    @Test
    fun testScaleBarAcrossDeviceRotation() {
        val iceland = LatLng(64.9631, -19.0208)
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        initScaleBar(initialZoom = 5f, initialPosition = iceland)
        composeTestRule.waitForIdle()

        val (portraitImperial, portraitMetric, portraitMeters) = computeExpectedLabels()
        composeTestRule.onNodeWithText(portraitImperial).assertExists()
        composeTestRule.onNodeWithText(portraitMetric).assertExists()

        // Rotate the device to landscape
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        composeTestRule.waitForIdle()

        val (landscapeImperial, landscapeMetric, landscapeMeters) = computeExpectedLabels()
        assertThat(landscapeImperial).isEqualTo(portraitImperial)
        assertThat(landscapeMetric).isEqualTo(portraitMetric)
        assertThat(abs(landscapeMeters - portraitMeters)).isAtMost(5)
        composeTestRule.onNodeWithText(landscapeImperial).assertExists()
        composeTestRule.onNodeWithText(landscapeMetric).assertExists()

        // Rotate the device back to portrait
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        composeTestRule.waitForIdle()

        val (restoredImperial, restoredMetric, restoredMeters) = computeExpectedLabels()
        assertThat(restoredImperial).isEqualTo(portraitImperial)
        assertThat(restoredMetric).isEqualTo(portraitMetric)
        assertThat(abs(restoredMeters - portraitMeters)).isAtMost(5)
        composeTestRule.onNodeWithText(restoredImperial).assertExists()
        composeTestRule.onNodeWithText(restoredMetric).assertExists()
    }

    @Test
    fun testScaleBarAcrossMapBearingRotation() {
        val iceland = LatLng(64.9631, -19.0208)
        initScaleBar(initialZoom = 10f, initialPosition = iceland)
        composeTestRule.waitForIdle()

        val (northUpImperial, northUpMetric, northUpMeters) = computeExpectedLabels()
        composeTestRule.onNodeWithText(northUpImperial).assertExists()
        composeTestRule.onNodeWithText(northUpMetric).assertExists()

        for (bearing in listOf(45f, 90f, 180f, 270f)) {
            composeTestRule.runOnIdle {
                cameraPositionState.move(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(iceland)
                            .zoom(10f)
                            .bearing(bearing)
                            .build(),
                    ),
                )
            }
            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                !cameraPositionState.isMoving &&
                    abs(cameraPositionState.position.bearing - bearing) < 1f
            }
            composeTestRule.waitForIdle()

            val (rotatedImperial, rotatedMetric, rotatedMeters) = computeExpectedLabels()
            assertThat(rotatedImperial).isEqualTo(northUpImperial)
            assertThat(rotatedMetric).isEqualTo(northUpMetric)
            assertThat(abs(rotatedMeters - northUpMeters)).isAtMost(10)
            composeTestRule.onNodeWithText(rotatedImperial).assertExists()
            composeTestRule.onNodeWithText(rotatedMetric).assertExists()
        }
    }

    @Test
    fun testScaleBarActivitySurvivesDeviceRotation() {
        assumeValidApiKey()
        ActivityScenario.launch(ScaleBarActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            composeTestRule.waitUntil(timeoutMillis = MAP_LOAD_TIMEOUT_SECONDS * 1_000) {
                composeTestRule.onAllNodesWithText("4.4 km").fetchSemanticsNodes().isNotEmpty() &&
                    composeTestRule.onAllNodesWithText("2.7 mi").fetchSemanticsNodes().isNotEmpty()
            }

            scenario.onActivity { activity ->
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            composeTestRule.waitUntil(timeoutMillis = MAP_LOAD_TIMEOUT_SECONDS * 1_000) {
                composeTestRule.onAllNodesWithText("4.4 km").fetchSemanticsNodes().isNotEmpty() &&
                    composeTestRule.onAllNodesWithText("2.7 mi").fetchSemanticsNodes().isNotEmpty()
            }

            scenario.onActivity { activity ->
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            composeTestRule.waitUntil(timeoutMillis = MAP_LOAD_TIMEOUT_SECONDS * 1_000) {
                composeTestRule.onAllNodesWithText("4.4 km").fetchSemanticsNodes().isNotEmpty() &&
                    composeTestRule.onAllNodesWithText("2.7 mi").fetchSemanticsNodes().isNotEmpty()
            }
        }
    }

    @Test
    fun testScaleBarLegacyTopLeftMeasurementWhenMeasureAtCenterIsFalse() {
        val iceland = LatLng(64.9631, -19.0208)
        initScaleBar(initialZoom = 5f, initialPosition = iceland, measureAtCenter = false)
        composeTestRule.waitForIdle()

        val (legacyImperial, legacyMetric, legacyMeters) = computeExpectedLabels(measureAtCenter = false)
        val (_, _, centerMeters) = computeExpectedLabels(measureAtCenter = true)
        assertThat(legacyMeters).isLessThan(centerMeters)
        composeTestRule.onNodeWithText(legacyImperial).assertExists()
        composeTestRule.onNodeWithText(legacyMetric).assertExists()
    }

    private fun computeExpectedLabels(measureAtCenter: Boolean = true): Triple<String, String, Int> {
        var imperialText = ""
        var metricText = ""
        var meters = 0

        composeTestRule.runOnIdle {
            val projection = cameraPositionState.projection
            projection?.let { proj ->
                val widthInDp = 65.dp
                val widthInPixels = with(density) {
                    widthInDp.toPx().toInt()
                }

                val startPoint: Point
                val endPoint: Point
                if (measureAtCenter) {
                    val centerPoint = proj.toScreenLocation(cameraPositionState.position.target)
                    val halfWidthInPixels = widthInPixels / 2
                    startPoint = Point(centerPoint.x - halfWidthInPixels, centerPoint.y)
                    endPoint = Point(startPoint.x + widthInPixels, centerPoint.y)
                } else {
                    startPoint = Point(0, 0)
                    endPoint = Point(widthInPixels, 0)
                }
                val startLatLng = proj.fromScreenLocation(startPoint)
                val endLatLng = proj.fromScreenLocation(endPoint)
                val canvasWidthMeters = startLatLng.sphericalDistance(endLatLng)
                val horizontalLineWidthMeters = (canvasWidthMeters * 8 / 9).toInt()
                meters = horizontalLineWidthMeters

                metricText = horizontalLineWidthMeters.meters.toDistanceString(
                    MetricUnitsConverter,
                    NumberFormatSymbols.COMPACT,
                )
                imperialText = horizontalLineWidthMeters.meters.toDistanceString(
                    scaleBarImperialConverter,
                    NumberFormatSymbols.COMPACT,
                )
            } ?: fail("Projection should not be null")
        }

        return Triple(imperialText, metricText, meters)
    }

    private fun assertScaleBarLabels(initialZoom: Float) {
        val initialPosition = LatLng(37.7749, -122.4194) // San Francisco
        initScaleBar(initialZoom, initialPosition)

        composeTestRule.waitForIdle()

        val (imperialText, metricText, _) = computeExpectedLabels()

        composeTestRule.onNodeWithText(
            text = imperialText,
        ).assertExists()
        composeTestRule.onNodeWithText(
            text = metricText,
        ).assertExists()
    }
}
