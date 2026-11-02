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

package com.google.maps.android.compose.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates the Baseline Profiles of the library modules by running the maps-app demos that
 * exercise them: the map itself, markers and Compose markers, clustering, shapes, overlays and
 * the ScaleBar widget.
 *
 * Run it with an emulator or device attached and a real MAPS_API_KEY in secrets.properties:
 * ```
 * ./gradlew :maps-compose:generateBaselineProfile \
 *   :maps-compose-utils:generateBaselineProfile \
 *   :maps-compose-widgets:generateBaselineProfile
 * ```
 * Each module keeps only its own classes (see the `baselineProfile` filter in its build file) and
 * the result is written to its `src/main/generated/baselineProfiles/baseline-prof.txt`, which is
 * packaged in the AAR.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(packageName = TARGET_PACKAGE) {
            pressHome()
            startActivityAndWait()
            DEMOS.forEach { demo -> exploreDemo(demo) }
        }
    }
}

/** Demos that cover the public API of maps-compose, maps-compose-utils and maps-compose-widgets. */
private val DEMOS = listOf(
    "BasicMapActivity",
    "MapInColumnActivity",
    "markerexamples.AdvancedMarkersActivity",
    "markerexamples.MarkerClusteringActivity",
    "markerexamples.markerscollection.MarkersCollectionActivity",
    "markerexamples.draggablemarkerscollectionwithpolygon.DraggableMarkersCollectionWithPolygonActivity",
    "TileOverlayActivity",
    "GroundOverlayActivity",
    "ScaleBarActivity",
)

/**
 * Opens [demo], waits for the map to load and pans and zooms it, so camera updates, recomposition
 * of the map content and re-clustering are part of the profile.
 */
internal fun MacrobenchmarkScope.exploreDemo(demo: String) {
    // startActivityAndWait relies on frame stats that the map's SurfaceView does not always
    // report, so the demos are started with `am start -W`, which waits for the launch instead.
    device.executeShellCommand("am start -W -n $packageName/.$demo")
    // The map loads asynchronously after the first frame, and there is no view to wait for.
    Thread.sleep(MAP_LOAD_DELAY_MS)

    val centerX = device.displayWidth / 2
    val centerY = device.displayHeight / 2
    val distance = device.displayWidth / 4
    device.swipe(centerX + distance, centerY, centerX - distance, centerY, SWIPE_STEPS)
    device.waitForIdle()
    device.swipe(centerX, centerY + distance, centerX, centerY - distance, SWIPE_STEPS)
    device.waitForIdle()
    // Double tap zooms in.
    device.click(centerX, centerY)
    device.click(centerX, centerY)
    Thread.sleep(CAMERA_ANIMATION_DELAY_MS)

    device.pressBack()
    device.waitForIdle()
}

internal const val TARGET_PACKAGE = "com.google.maps.android.compose"

private const val MAP_LOAD_DELAY_MS = 3_000L
private const val CAMERA_ANIMATION_DELAY_MS = 1_000L
private const val SWIPE_STEPS = 20
