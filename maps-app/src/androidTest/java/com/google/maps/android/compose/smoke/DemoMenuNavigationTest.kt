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

import android.app.Activity
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.StreetViewPanoramaView
import com.google.common.truth.Truth.assertWithMessage
import com.google.maps.android.compose.ActivityGroup
import com.google.maps.android.compose.MainActivity
import com.google.maps.android.compose.StreetViewActivity
import com.google.maps.android.compose.allActivityGroups
import com.google.maps.android.compose.demoTestTag
import com.google.maps.android.compose.groupTestTag
import com.google.maps.android.compose.hasValidApiKey
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import com.google.maps.android.compose.Activity as Demo

/**
 * Walks the sample app the way a user does: starts [MainActivity], expands the demo's group,
 * taps the demo, waits for its map or Street View panorama, and presses back to return to the
 * menu. A crash on a background thread at any point fails the test.
 *
 * [DemoAppSmokeTest] launches each demo directly to check it in depth. This test covers the path
 * a reviewer actually takes, so a broken menu entry, click handler or back navigation fails here.
 * The parameters come from [allActivityGroups], so new demos are covered automatically.
 */
@RunWith(Parameterized::class)
class DemoMenuNavigationTest(
    private val group: ActivityGroup,
    private val demo: Demo,
    @Suppress("unused") private val name: String,
) {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private val uncaughtExceptions = CopyOnWriteArrayList<Throwable>()
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    @Before
    fun setUp() {
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
            defaultHandler?.let { Thread.setDefaultUncaughtExceptionHandler(it) }
        }
    }

    @Test
    fun opensFromMenuAndNavigatesBack() {
        val demoClass = demo.kClass.java

        // Scroll the list to each node first: groups and demos below the fold, on small screens
        // or in landscape, are not composed until the LazyColumn scrolls to them.
        val menu = composeRule.onNode(hasScrollToIndexAction())
        menu.performScrollToNode(hasTestTag(groupTestTag(group)))
        composeRule.onNodeWithTag(groupTestTag(group)).performClick()
        menu.performScrollToNode(hasTestTag(demoTestTag(demo)))
        composeRule.onNodeWithTag(demoTestTag(demo)).performClick()

        val activity = waitForResumedActivity(demoClass)
        // Maps become ready without a valid key, but StreetViewActivity only shows its panorama
        // after a Street View metadata request succeeds, which needs one. Without a key that demo
        // is only checked for opening, so the test still runs on forks and locally.
        if (hasValidApiKey || demoClass != StreetViewActivity::class.java) {
            awaitMapSurfaceReady(activity)
        }
        assertNoUncaughtExceptions()

        assertWithMessage("${demoClass.simpleName} finished by itself")
            .that(activity.isFinishing)
            .isFalse()

        Espresso.pressBackUnconditionally()
        waitForResumedActivity(MainActivity::class.java)
    }

    private fun resumedActivity(): Activity? {
        var resumed: Activity? = null
        instrumentation.runOnMainSync {
            resumed = ActivityLifecycleMonitorRegistry
                .getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .firstOrNull()
        }
        return resumed
    }

    private fun waitForResumedActivity(activityClass: Class<out Activity>): Activity {
        val deadline = SystemClock.uptimeMillis() + TIMEOUT_MS
        while (SystemClock.uptimeMillis() < deadline) {
            val resumed = resumedActivity()
            if (resumed != null && resumed.javaClass == activityClass) {
                return resumed
            }
            SystemClock.sleep(POLL_MS)
        }
        throw AssertionError(
            "${activityClass.simpleName} was not resumed within ${TIMEOUT_MS}ms, " +
                "the foreground activity is ${resumedActivity()?.javaClass?.simpleName}"
        )
    }

    /**
     * Finds the first [MapView] or [StreetViewPanoramaView] in [activity] and waits for its map or
     * panorama to be ready.
     */
    private fun awaitMapSurfaceReady(activity: Activity) {
        val deadline = SystemClock.uptimeMillis() + TIMEOUT_MS
        val ready = CountDownLatch(1)
        var requested = false
        while (!requested && SystemClock.uptimeMillis() < deadline) {
            // The compose rule drives the frame clock of every composition in the process, so
            // advance it: StreetViewActivity only shows its panorama after a recomposition that
            // follows an async metadata request.
            composeRule.mainClock.advanceTimeByFrame()
            instrumentation.runOnMainSync {
                when (val surface = findMapSurface(activity.window.decorView)) {
                    is MapView -> surface.getMapAsync { ready.countDown() }
                    is StreetViewPanoramaView ->
                        surface.getStreetViewPanoramaAsync { ready.countDown() }
                    else -> return@runOnMainSync
                }
                requested = true
            }
            if (!requested) SystemClock.sleep(POLL_MS)
        }
        val name = activity.javaClass.simpleName
        assertWithMessage("No MapView or StreetViewPanoramaView found in $name")
            .that(requested)
            .isTrue()
        assertWithMessage("Map surface in $name was not ready within ${TIMEOUT_MS}ms")
            .that(ready.await(TIMEOUT_MS, TimeUnit.MILLISECONDS))
            .isTrue()
    }

    private fun findMapSurface(view: View): View? {
        if (view is MapView || view is StreetViewPanoramaView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findMapSurface(view.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    private fun assertNoUncaughtExceptions() {
        val failure = uncaughtExceptions.firstOrNull() ?: return
        val name = demo.kClass.java.simpleName
        throw AssertionError("$name crashed on a background thread", failure)
    }

    companion object {
        private const val TIMEOUT_MS = 15_000L
        private const val POLL_MS = 100L

        @JvmStatic
        @Parameterized.Parameters(name = "{2}")
        fun demos(): List<Array<Any>> =
            allActivityGroups.flatMap { group ->
                group.activities.map { demo -> arrayOf(group, demo, demo.kClass.java.simpleName) }
            }
    }
}
