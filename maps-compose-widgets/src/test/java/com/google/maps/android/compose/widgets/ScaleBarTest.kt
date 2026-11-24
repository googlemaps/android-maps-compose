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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.common.truth.Truth.assertThat
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.robolectric.annotation.EnableMapsShadows
import com.google.maps.android.robolectric.ktx.createTestMap
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
    internal fun testCalculateDistance_computesExpectedCanvasDistance() {
        val projection = mockk<Projection>()
        every { projection.fromScreenLocation(Point(0, 0)) } returns LatLng(0.0, 0.0)
        every { projection.fromScreenLocation(Point(100, 0)) } returns LatLng(0.0, 0.001)

        val result = calculateDistance(
            projection = projection,
            width = 100.dp,
            density = Density(1f, 1f),
        )

        // 0.001 deg of longitude at the equator is ~111.19 m; the drawn scale bar spans 8/9 of the canvas (~98 m).
        assertThat(result).isEqualTo(98)
    }

    @Test
    internal fun testCalculateDistance_atTarget_computesExpectedCenterSpanDistance() {
        val equator = LatLng(0.0, 0.0)
        val highLatitude = LatLng(60.0, 0.0)
        val density = Density(1f, 1f)
        val width = 100.dp

        val equatorMap = createTestMap().apply {
            moveCamera(CameraUpdateFactory.newLatLngZoom(equator, 12f))
        }
        val equatorDistance = calculateDistance(equatorMap.projection, width, density, target = equator)

        // At zoom 12 on the equator, 100 px spans ~3,822 m; 8/9 of that is ~3,397 m.
        assertThat(equatorDistance).isIn(3350..3450)

        val highLatMap = createTestMap().apply {
            moveCamera(CameraUpdateFactory.newLatLngZoom(highLatitude, 12f))
        }
        val highLatDistance = calculateDistance(highLatMap.projection, width, density, target = highLatitude)

        // At 60 deg latitude, cos(60 deg) = 0.5, so the same pixel width spans half the ground distance (~1,699 m).
        assertThat(highLatDistance).isIn(1650..1750)
    }

    @Test
    internal fun testScaleBar_rendersWithoutCrashing() {
        val cameraState = CameraPositionState(
            position = CameraPosition.fromLatLngZoom(LatLng(37.7749, -122.4194), 12f),
        )

        composeTestRule.setContent {
            ScaleBar(
                cameraPositionState = cameraState,
                width = 80.dp,
                height = 50.dp,
                textColor = Color.Black,
                lineColor = Color.Blue,
                shadowColor = Color.White,
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("0 ft").assertIsDisplayed()
        composeTestRule.onNodeWithText("0 m").assertIsDisplayed()
    }

    @Test
    internal fun testDisappearingScaleBar_showsOnCameraChangeAndHidesAfterDuration() {
        composeTestRule.mainClock.autoAdvance = false
        val cameraState = CameraPositionState(
            position = CameraPosition.fromLatLngZoom(LatLng(37.7749, -122.4194), 10f),
        )

        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                DisappearingScaleBar(
                    cameraPositionState = cameraState,
                    visibilityDurationMillis = 1_000,
                )
            }
        }

        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.onNodeWithText("0 m").assertIsDisplayed()

        // Advance past visibilityDurationMillis (1,000 ms) and the exit fade animation.
        composeTestRule.mainClock.advanceTimeBy(2_000L)
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.onNodeWithText("0 m").assertDoesNotExist()

        // Updating camera position re-triggers the LaunchedEffect and shows the scale bar again.
        Snapshot.withMutableSnapshot {
            cameraState.position = CameraPosition.fromLatLngZoom(LatLng(37.7749, -122.4194), 14f)
        }
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.mainClock.advanceTimeBy(200L)
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.onNodeWithText("0 m").assertIsDisplayed()

        // Advance past visibilityDurationMillis again and confirm the scale bar hides.
        composeTestRule.mainClock.advanceTimeBy(2_000L)
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.onNodeWithText("0 m").assertDoesNotExist()
    }
}
// [END maps_compose_widgets_scale_bar_test]
