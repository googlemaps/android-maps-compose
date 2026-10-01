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

package com.google.maps.android.compose.smoke

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.StreetViewPanoramaView
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.maps.android.compose.allActivityGroups
import com.google.maps.android.compose.hasValidApiKey
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * End-to-end smoke test covering every demo in the sample app.
 *
 * The parameter list is derived from [allActivityGroups], the same registry that builds the
 * demo list on screen, so a demo added to the app is covered here automatically with no
 * change to this file. That is the point of the test: it is a standing guarantee that every
 * screen a reviewer would otherwise open by hand still launches, renders a map and survives a
 * configuration change.
 *
 * Each demo is checked for three things:
 * - it reaches [Lifecycle.State.RESUMED] without throwing,
 * - a map surface ([MapView] or [StreetViewPanoramaView]) is attached and laid out,
 * - its map survives a zoom out and back in, which exercises the camera listeners the demos and
 *   the library hook into,
 * - nothing crashes on a background thread while it is open.
 *
 * This deliberately does not assert on map *content*. Verifying that a particular marker or
 * overlay is drawn belongs in the focused tests next to this one; the value here is breadth.
 */
@RunWith(Parameterized::class)
class DemoAppSmokeTest(
    private val demoName: String,
    private val demoActivity: Class<ComponentActivity>,
) {

    companion object {
        /**
         * How long to wait for a demo's map surface to be attached and laid out. Generous
         * because the first demo to run on a cold emulator pays for Maps SDK initialisation.
         */
        private const val MAP_SURFACE_TIMEOUT_MS = 20_000L

        private const val POLL_INTERVAL_MS = 250L

        /** How long to wait for tiles to render when `requireMapLoaded` is set. */
        private const val MAP_LOADED_TIMEOUT_MS = 30_000L

        /** Time given to a demo to react to a camera change before the next check. */
        private const val SETTLE_MS = 1_500L

        /**
         * Demos that legitimately show no map surface of their own. Keep this empty unless a
         * demo really is map-free; an entry here is a hole in the coverage, not a fix.
         */
        private val DEMOS_WITHOUT_MAP_SURFACE = emptySet<String>()

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun demos(): List<Array<Any>> =
            allActivityGroups
                .flatMap { group -> group.activities }
                .map { activity ->
                    @Suppress("UNCHECKED_CAST")
                    arrayOf(
                        activity.kClass.simpleName ?: activity.kClass.java.name,
                        activity.kClass.java as Class<ComponentActivity>,
                    )
                }
    }

    /**
     * Tiles only render with a real key, so waiting for them is opt-in: pass
     * `-Pandroid.testInstrumentationRunnerArguments.requireMapLoaded=true`. CI does when the key
     * secret is available.
     */
    private val requireMapLoaded =
        InstrumentationRegistry.getArguments().getString("requireMapLoaded").toBoolean()

    private val uncaughtExceptions = CopyOnWriteArrayList<Throwable>()
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    @Before
    fun setUp() {
        // Without a key the Maps SDK renders an empty grid, so every map assertion below would
        // be meaningless. Skip rather than fail: forks run CI without access to the secret.
        assumeTrue("Maps API key not specified", hasValidApiKey)

        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            uncaughtExceptions.add(throwable)
        }
    }

    /** Also catches crashes while the demo is torn down after the test body. */
    @After
    fun tearDown() {
        try {
            assertNoUncaughtExceptions()
        } finally {
            // Only restore what setUp replaced: if the key check skipped the test, defaultHandler
            // was never read, and restoring null would disable crash reporting for later tests.
            defaultHandler?.let { Thread.setDefaultUncaughtExceptionHandler(it) }
        }
    }

    @Test
    fun demoLaunchesAndShowsMap() {
        ActivityScenario.launch(demoActivity).use { scenario ->
            scenario.assertResumed()
            if (demoName !in DEMOS_WITHOUT_MAP_SURFACE) {
                scenario.awaitMapSurface()
                scenario.zoomOutAndBackIn()
            }
            scenario.assertResumed()
            assertNoUncaughtExceptions()
        }
    }

    /**
     * Rotation and process-level configuration changes are where camera and marker state
     * holders tend to regress, and they are easy to miss when clicking through the app by hand.
     */
    @Test
    fun demoSurvivesConfigurationChange() {
        ActivityScenario.launch(demoActivity).use { scenario ->
            scenario.assertResumed()
            if (demoName !in DEMOS_WITHOUT_MAP_SURFACE) {
                scenario.awaitMapSurface()
            }

            scenario.recreate()

            scenario.assertResumed()
            if (demoName !in DEMOS_WITHOUT_MAP_SURFACE) {
                scenario.awaitMapSurface()
            }
            assertNoUncaughtExceptions()
        }
    }

    private fun ActivityScenario<ComponentActivity>.assertResumed() {
        assertThat(state).isEqualTo(Lifecycle.State.RESUMED)
    }

    /**
     * Polls until the demo has a map surface that is attached, visible and non-zero sized.
     * Polling rather than a single check because [MapView] is created from an `AndroidView`
     * factory and laid out a frame or more after the activity resumes.
     */
    private fun ActivityScenario<ComponentActivity>.awaitMapSurface() {
        val deadline = System.currentTimeMillis() + MAP_SURFACE_TIMEOUT_MS
        var surfaces = emptyList<View>()

        while (System.currentTimeMillis() < deadline) {
            onActivity { activity ->
                surfaces = activity.window.decorView.mapSurfaces()
            }
            if (surfaces.any { it.isShown && it.width > 0 && it.height > 0 }) return
            Thread.sleep(POLL_INTERVAL_MS)
        }

        val detail = if (surfaces.isEmpty()) {
            "no MapView or StreetViewPanoramaView was found in the view hierarchy"
        } else {
            surfaces.joinToString(prefix = "found but not laid out: ") { surface ->
                "${surface.javaClass.simpleName}" +
                    "(shown=${surface.isShown}, ${surface.width}x${surface.height})"
            }
        }
        throw AssertionError(
            "$demoName did not show a map within ${MAP_SURFACE_TIMEOUT_MS}ms: $detail",
        )
    }

    /**
     * Zooms the demo's map out and back in. When [requireMapLoaded] is set, also waits for the
     * tiles to render each time. The `GoogleMap` composable registers its loaded callback once,
     * so replacing it means a demo's own `onMapLoaded` stops firing for the rest of the test,
     * which only affects UI such as loading indicators. Demos showing Street View are skipped.
     *
     * A map that is fully on screen is preferred: in demos with several maps in a scrolling list,
     * a map partly off screen may never report that it finished loading.
     */
    private fun ActivityScenario<ComponentActivity>.zoomOutAndBackIn() {
        var mapView: MapView? = null
        onActivity { activity ->
            val mapViews = activity.window.decorView.mapSurfaces().filterIsInstance<MapView>()
            mapView = mapViews.firstOrNull { it.isFullyOnScreen() } ?: mapViews.firstOrNull()
        }
        val view = mapView ?: return

        var map: GoogleMap? = null
        val ready = CountDownLatch(1)
        onActivity {
            view.getMapAsync {
                map = it
                ready.countDown()
            }
        }
        assertWithMessage("$demoName map was not ready within ${MAP_SURFACE_TIMEOUT_MS}ms")
            .that(ready.await(MAP_SURFACE_TIMEOUT_MS, TimeUnit.MILLISECONDS))
            .isTrue()

        for (delta in floatArrayOf(-1f, 1f)) {
            val loaded = CountDownLatch(1)
            onActivity {
                if (requireMapLoaded) map!!.setOnMapLoadedCallback { loaded.countDown() }
                map!!.moveCamera(CameraUpdateFactory.zoomBy(delta))
            }
            if (requireMapLoaded) {
                assertWithMessage("$demoName map did not finish loading after zooming")
                    .that(loaded.await(MAP_LOADED_TIMEOUT_MS, TimeUnit.MILLISECONDS))
                    .isTrue()
            }
            Thread.sleep(SETTLE_MS)
        }
    }

    private fun assertNoUncaughtExceptions() {
        val failure = uncaughtExceptions.firstOrNull() ?: return
        throw AssertionError("$demoName crashed on a background thread", failure)
    }

    /** True when this view is shown and its whole area is visible on screen. */
    private fun View.isFullyOnScreen(): Boolean {
        if (!isShown || width == 0 || height == 0) return false
        val visible = Rect()
        return getGlobalVisibleRect(visible) &&
            visible.width() == width &&
            visible.height() == height
    }

    /** Depth-first walk collecting every Maps SDK surface below this view. */
    private fun View.mapSurfaces(): List<View> = buildList {
        fun walk(view: View) {
            if (view is MapView || view is StreetViewPanoramaView) add(view)
            if (view is ViewGroup) {
                for (index in 0 until view.childCount) walk(view.getChildAt(index))
            }
        }
        walk(this@mapSurfaces)
    }
}
