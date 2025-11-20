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

import android.graphics.Point
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.widgets.ScaleBar
import com.google.maps.android.ktx.utils.sphericalDistance
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

// These constants are used for converting between metric and imperial units
// to ensure the scale bar displays distances correctly in both systems.
private const val CENTIMETERS_IN_METER: Double = 100.0
private const val METERS_IN_KILOMETER: Double = 1000.0
private const val CENTIMETERS_IN_INCH: Double = 2.54
private const val INCHES_IN_FOOT: Double = 12.0
private const val FEET_IN_MILE: Double = 5280.0

class ScaleBarTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var cameraPositionState: CameraPositionState

    private fun initScaleBar(initialZoom: Float, initialPosition: LatLng) {
        check(hasValidApiKey) { "Maps API key not specified" }

        val countDownLatch = CountDownLatch(1)

        cameraPositionState = CameraPositionState(
            position = CameraPosition.fromLatLngZoom(initialPosition, initialZoom)
        )

        composeTestRule.setContent {
            Box {
                GoogleMap(
                    cameraPositionState = cameraPositionState,
                    onMapLoaded = {
                        countDownLatch.countDown()
                    }
                )
                ScaleBar(cameraPositionState = cameraPositionState)
            }
        }
        val mapLoaded = countDownLatch.await(MAP_LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        assertTrue(mapLoaded)
    }

    @Test
    fun testScaleBarInitialState() {
        val initialZoom = 15f
        val initialPosition = LatLng(37.7749, -122.4194) // San Francisco
        initScaleBar(initialZoom, initialPosition)

        composeTestRule.waitForIdle()

        var imperialText = ""
        var metricText = ""

        composeTestRule.runOnIdle {
            // We use a `let` block to safely handle the projection, which can be null.
            // If the projection is null, the test will fail explicitly, preventing
            // any potential NullPointerExceptions and ensuring the test is robust.
            val projection = cameraPositionState.projection
            projection?.let { proj ->
                val widthInDp = 65.dp
                val widthInPixels = widthInDp.value.toInt()

                val upperLeftLatLng = proj.fromScreenLocation(Point(0, 0))
                val upperRightLatLng = proj.fromScreenLocation(Point(0, widthInPixels))
                val canvasWidthMeters = upperLeftLatLng.sphericalDistance(upperRightLatLng)
                val horizontalLineWidthMeters = (canvasWidthMeters * 8 / 9).toInt()

                var metricUnits = "m"
                var metricDistance = horizontalLineWidthMeters
                if (horizontalLineWidthMeters > METERS_IN_KILOMETER) {
                    metricUnits = "km"
                    metricDistance /= METERS_IN_KILOMETER.toInt()
                }

                var imperialUnits = "ft"
                var imperialDistance = horizontalLineWidthMeters.toDouble().toFeet()
                if (imperialDistance > FEET_IN_MILE) {
                    imperialUnits = "mi"
                    imperialDistance = imperialDistance.toMiles()
                }
                imperialText = "${imperialDistance.toInt()} $imperialUnits"
                metricText = "$metricDistance $metricUnits"
            } ?: fail("Projection should not be null")
        }

        composeTestRule.onNodeWithText(
            text = imperialText,
        ).assertExists()
        composeTestRule.onNodeWithText(
            text = metricText,
        ).assertExists()
    }
}

internal fun Double.toFeet(): Double {
    return this * CENTIMETERS_IN_METER / CENTIMETERS_IN_INCH / INCHES_IN_FOOT
}

internal fun Double.toMiles(): Double {
    return this / FEET_IN_MILE
}
