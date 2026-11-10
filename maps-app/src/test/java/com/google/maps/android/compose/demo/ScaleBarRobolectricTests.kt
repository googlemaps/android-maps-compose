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

package com.google.maps.android.compose.demo

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.widgets.ScaleBar
import com.google.maps.android.robolectric.annotation.EnableMapsShadows
import com.google.maps.android.robolectric.ktx.getMapBlocking
import com.google.maps.android.robolectric.ktx.polylines
import com.google.maps.android.robolectric.ktx.requireMapView
import com.google.maps.android.robolectric.truth.assertThat
import com.google.maps.android.sphericalDistance
import com.google.maps.android.testing.golden.GoldenImageDiff
import com.google.maps.android.testing.golden.GoldenTileStrategy
import com.google.maps.android.testing.golden.captureSnapshot
import com.google.maps.android.testing.golden.enableGoldenTesting
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.google.common.truth.Truth.assertThat as truthAssertThat
import com.google.maps.android.robolectric.truth.meters as toleranceMeters

/**
 * Headless JVM unit and golden snapshot tests for [ScaleBar] and [ScaleBarActivity] powered by
 * the Android Maps Testing Toolkit (`maps-testing-robolectric` and `maps-testing-golden`).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
@EnableMapsShadows
class ScaleBarRobolectricTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var cameraPositionState: CameraPositionState

    @Test
    fun scaleBarUpdatesAcrossMercatorLatitudeChangesOnHeadlessMap() {
        val equator = LatLng(0.0, 0.0)
        val iceland = LatLng(64.9631, -19.0208)
        cameraPositionState = CameraPositionState(
            position = CameraPosition.fromLatLngZoom(equator, 10f),
        )
        composeTestRule.setContent {
            Box(modifier = Modifier.size(400.dp)) {
                GoogleMap(
                    modifier = Modifier.matchParentSize(),
                    cameraPositionState = cameraPositionState,
                )
                ScaleBar(cameraPositionState = cameraPositionState)
            }
        }
        composeTestRule.waitForIdle()
        val map = composeTestRule.activity.window.decorView.requireMapView().getMapBlocking()

        // At the Equator (0°, zoom 10), a 65 dp bar spans 8,822 m -> "8.8 km" / "5.5 mi"
        assertThat(map)
            .isCenteredOn(equator, tolerance = 5.toleranceMeters)
            .hasZoom(10f)
        composeTestRule.onNodeWithText("8.8 km").assertIsDisplayed()
        composeTestRule.onNodeWithText("5.5 mi").assertIsDisplayed()

        // Pan to Iceland (64.96°N) at the exact same zoom (10f); Mercator cos(64.96°) = 0.423
        // shrinks the ground distance to 3,733 m -> "3.7 km" / "2.3 mi"
        composeTestRule.runOnIdle {
            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(iceland, 10f))
        }
        composeTestRule.waitForIdle()

        assertThat(map)
            .isCenteredOn(iceland, tolerance = 5.toleranceMeters)
            .hasZoom(10f)
        composeTestRule.onNodeWithText("3.7 km").assertIsDisplayed()
        composeTestRule.onNodeWithText("2.3 mi").assertIsDisplayed()
    }

    @Test
    fun scaleBarReactsWhenMeasureAtCenterIsToggledOnHeadlessMap() {
        val iceland = LatLng(64.9631, -19.0208)
        var measureAtCenter by mutableStateOf(true)
        cameraPositionState = CameraPositionState(
            position = CameraPosition.fromLatLngZoom(iceland, 5f),
        )
        composeTestRule.setContent {
            Box(modifier = Modifier.size(400.dp)) {
                GoogleMap(
                    modifier = Modifier.matchParentSize(),
                    cameraPositionState = cameraPositionState,
                )
                ScaleBar(
                    cameraPositionState = cameraPositionState,
                    measureAtCenter = measureAtCenter,
                )
            }
        }
        composeTestRule.waitForIdle()

        // 1. Default measureAtCenter = true at Iceland (64.96°N, y = 200): 119,474 m -> "119 km" / "74 mi"
        composeTestRule.onNodeWithText("119 km").assertIsDisplayed()
        composeTestRule.onNodeWithText("74 mi").assertIsDisplayed()

        // 2. Flip measureAtCenter = false on the live ScaleBar: measures at Point(0, 0) (at the top
        //    of the 400 px viewport), shrinking the ground distance to 103,770 m -> "104 km" / "64 mi"
        composeTestRule.runOnIdle {
            measureAtCenter = false
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("104 km").assertIsDisplayed()
        composeTestRule.onNodeWithText("64 mi").assertIsDisplayed()

        // 3. Flip measureAtCenter back to true and verify it restores "119 km" / "74 mi"
        composeTestRule.runOnIdle {
            measureAtCenter = true
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("119 km").assertIsDisplayed()
        composeTestRule.onNodeWithText("74 mi").assertIsDisplayed()
    }

    @Test
    fun scaleBarActivityRendersReferenceCrosshairsAndRespondsToToggles() = runTest {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            ScaleBarActivity::class.java,
        ).apply {
            putExtra("lat", 64.9631f)
            putExtra("lng", -19.0208f)
            putExtra("zoom", 5f)
        }

        ActivityScenario.launch<ScaleBarActivity>(intent).use { scenario ->
            composeTestRule.waitForIdle()

            lateinit var map: GoogleMap
            scenario.onActivity { activity ->
                val mapView = activity.window.decorView.requireMapView()
                map = mapView.getMapBlocking()
            }

            // 1. Verify production activity map state, initial center-measured labels ("119 km" / "74 mi"),
            //    and 3 East-West + 3 North-South reference crosshairs (6 polylines total)
            assertThat(map)
                .isCenteredOn(LatLng(64.9631, -19.0208), tolerance = 10.toleranceMeters)
                .hasZoom(5f)
                .hasPolylineCount(6)
            truthAssertThat(
                composeTestRule.onAllNodesWithText("119 km").fetchSemanticsNodes(),
            ).isNotEmpty()
            truthAssertThat(
                composeTestRule.onAllNodesWithText("74 mi").fetchSemanticsNodes(),
            ).isNotEmpty()

            val centerCrosshairSpanMeters = map.polylines.first().let { polyline ->
                polyline.points.first().sphericalDistance(polyline.points.last())
            }
            truthAssertThat(centerCrosshairSpanMeters).isWithin(500.0).of(119_474.0)

            // 2. Verify deterministic golden snapshot rendering of the crosshairs overlay
            map.enableGoldenTesting(strategy = GoldenTileStrategy.SolidColorHash())
            val baselineWithCrosshairs = map.captureSnapshot()
            val repeatedWithCrosshairs = map.captureSnapshot()
            GoldenImageDiff.assertMatches(
                expected = baselineWithCrosshairs,
                actual = repeatedWithCrosshairs,
                maxDiffPercentage = 0.0,
            )

            // 3. Toggle "Measure at center" off and verify both the ScaleBar labels and crosshair
            //    polyline lengths (~101-103 km at the top of the window) shrink from 119,474 m
            composeTestRule.onNodeWithText("Measure at center").performClick()
            composeTestRule.waitForIdle()
            truthAssertThat(
                composeTestRule.onAllNodesWithText("119 km").fetchSemanticsNodes(),
            ).isEmpty()
            truthAssertThat(
                composeTestRule.onAllNodesWithText("74 mi").fetchSemanticsNodes(),
            ).isEmpty()
            val topLeftCrosshairSpanMeters = map.polylines.first().let { polyline ->
                polyline.points.first().sphericalDistance(polyline.points.last())
            }
            truthAssertThat(topLeftCrosshairSpanMeters).isWithin(2_000.0).of(102_000.0)

            // 4. Toggle "Show crosshairs" off and verify all 6 polylines are removed and visual diff is detected
            composeTestRule.onNodeWithText("Show crosshairs").performClick()
            composeTestRule.waitForIdle()
            assertThat(map).hasPolylineCount(0)

            val snapshotWithoutCrosshairs = map.captureSnapshot()
            val diff = GoldenImageDiff.compare(baselineWithCrosshairs, snapshotWithoutCrosshairs)
            truthAssertThat(diff.matches).isFalse()
            truthAssertThat(diff.diffPixelCount).isGreaterThan(0)
        }
    }
}
