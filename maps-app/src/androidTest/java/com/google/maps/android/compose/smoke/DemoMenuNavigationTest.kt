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
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.google.android.gms.maps.MapView
import com.google.common.truth.Truth.assertWithMessage
import com.google.maps.android.compose.ActivityGroup
import com.google.maps.android.compose.MainActivity
import com.google.maps.android.compose.StreetViewActivity
import com.google.maps.android.compose.allActivityGroups
import com.google.maps.android.compose.demoTestTag
import com.google.maps.android.compose.groupTestTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import com.google.maps.android.compose.Activity as Demo

/**
 * Walks the sample app the way a user does: starts [MainActivity], expands the demo's group,
 * taps the demo, waits for its map, and presses back to return to the menu.
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

    @Test
    fun opensFromMenuAndNavigatesBack() {
        val demoClass = demo.kClass.java

        composeRule.onNodeWithTag(groupTestTag(group)).performClick()
        composeRule.onNodeWithTag(demoTestTag(demo)).performScrollTo().performClick()

        val activity = waitForResumedActivity(demoClass)
        if (demoClass !in DEMOS_WITHOUT_MAP) {
            awaitMapReady(activity)
        }

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

    /** Finds the first [MapView] in [activity] and waits for its map to be ready. */
    private fun awaitMapReady(activity: Activity) {
        val deadline = SystemClock.uptimeMillis() + TIMEOUT_MS
        val ready = CountDownLatch(1)
        var requested = false
        while (!requested && SystemClock.uptimeMillis() < deadline) {
            instrumentation.runOnMainSync {
                val mapView = findMapView(activity.window.decorView) ?: return@runOnMainSync
                mapView.getMapAsync { ready.countDown() }
                requested = true
            }
            if (!requested) SystemClock.sleep(POLL_MS)
        }
        assertWithMessage("No MapView found in ${activity.javaClass.simpleName}")
            .that(requested)
            .isTrue()
        assertWithMessage("Map in ${activity.javaClass.simpleName} was not ready within ${TIMEOUT_MS}ms")
            .that(ready.await(TIMEOUT_MS, TimeUnit.MILLISECONDS))
            .isTrue()
    }

    private fun findMapView(view: View): MapView? {
        if (view is MapView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findMapView(view.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    companion object {
        private const val TIMEOUT_MS = 15_000L
        private const val POLL_MS = 100L

        /** Demos that show something other than a map. */
        private val DEMOS_WITHOUT_MAP: Set<Class<out Activity>> = setOf(
            StreetViewActivity::class.java,
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{2}")
        fun demos(): List<Array<Any>> =
            allActivityGroups.flatMap { group ->
                group.activities.map { demo -> arrayOf(group, demo, demo.kClass.java.simpleName) }
            }
    }
}
