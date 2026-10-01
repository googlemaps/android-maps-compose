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

package com.google.maps.android.compose.widgets

import android.graphics.Point
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.maps.robolectric.annotation.EnableMapsShadows
import com.google.common.truth.Truth.assertThat
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.ktx.utils.sphericalDistance
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// [START maps_compose_widgets_scale_bar_test]
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@EnableMapsShadows
internal class ScaleBarTest {

    @get:Rule
    internal val composeTestRule: AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity> =
        createAndroidComposeRule<ComponentActivity>()

    @Test
    internal fun testCalculateDistance_computesExpectedCanvasDistance(): Unit {
        val projection = mockk<Projection>(relaxed = true)
        val density = Density(1f, 1f)
        val width = 100.dp

        val startPoint = Point(0, 0)
        val endPoint = Point(width.value.toInt(), 0)

        val startLatLng = LatLng(0.0, 0.0)
        val endLatLng = LatLng(0.0, 0.001)

        every { projection.fromScreenLocation(startPoint) } returns startLatLng
        every { projection.fromScreenLocation(endPoint) } returns endLatLng

        val expectedDistance = startLatLng.sphericalDistance(endLatLng)
        val expectedResult = (expectedDistance * 8 / 9).toInt()

        val result = calculateDistance(projection, width, density)

        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    internal fun testUnitConversions_toFeetAndToMiles(): Unit {
        val meters = 1609.344 // 1 mile in meters
        val feet = meters.toFeet()
        assertThat(feet).isWithin(1.0).of(5280.0)

        val miles = feet.toMiles()
        assertThat(miles).isWithin(0.01).of(1.0)
    }

    @Test
    internal fun testScaleBar_rendersWithoutCrashing(): Unit {
        val cameraState = CameraPositionState(
            position = CameraPosition.fromLatLngZoom(LatLng(37.7749, -122.4194), 12f)
        )

        composeTestRule.setContent {
            ScaleBar(
                cameraPositionState = cameraState,
                width = 80.dp,
                height = 50.dp,
                textColor = Color.Black,
                lineColor = Color.Blue,
                shadowColor = Color.White
            )
        }

        composeTestRule.waitForIdle()
    }

    @Test
    internal fun testDisappearingScaleBar_rendersAndRespondsToCameraPosition(): Unit {
        val cameraState = CameraPositionState(
            position = CameraPosition.fromLatLngZoom(LatLng(37.7749, -122.4194), 10f)
        )

        composeTestRule.setContent {
            DisappearingScaleBar(
                cameraPositionState = cameraState,
                visibilityDurationMillis = 500
            )
        }

        composeTestRule.waitForIdle()

        // Update camera position to trigger visibility LaunchedEffect
        cameraState.position = CameraPosition.fromLatLngZoom(LatLng(37.7749, -122.4194), 14f)
        composeTestRule.waitForIdle()
    }
}
// [END maps_compose_widgets_scale_bar_test]
