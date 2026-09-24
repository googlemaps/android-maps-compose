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

package com.google.maps.android.compose

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Guards the demo registry itself.
 *
 * A demo is described in two places that have to agree: [allActivityGroups], which builds the
 * on-screen list, and `AndroidManifest.xml`, which declares the activity. Adding a demo to one
 * and not the other builds cleanly and only fails when someone taps the entry, which is exactly
 * the kind of breakage a reviewer has to catch by hand today.
 */
class DemoRegistryTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val demos = allActivityGroups.flatMap { group -> group.activities }

    @Test
    fun everyDemoIsDeclaredInTheManifest() {
        val undeclared = demos.filterNot { demo ->
            val component = ComponentName(context, demo.kClass.java)
            runCatching {
                context.packageManager.getActivityInfo(component, 0)
            }.isSuccess
        }

        assertThat(undeclared.map { it.kClass.simpleName }).isEmpty()
    }

    /**
     * The demo list launches each entry with a bare [android.content.Intent], which only works
     * for activities the system will start from the sample's own task.
     */
    @Test
    fun everyDemoIsLaunchable() {
        val notLaunchable = demos.filterNot { demo ->
            val component = ComponentName(context, demo.kClass.java)
            val info = runCatching {
                context.packageManager.getActivityInfo(component, PackageManager.MATCH_ALL)
            }.getOrNull()
            info != null && info.isEnabled
        }

        assertThat(notLaunchable.map { it.kClass.simpleName }).isEmpty()
    }

    @Test
    fun everyDemoHasATitleAndDescription() {
        val missingStrings = demos.filter { demo ->
            context.getString(demo.title).isBlank() || context.getString(demo.description).isBlank()
        }

        assertThat(missingStrings.map { it.kClass.simpleName }).isEmpty()
    }

    @Test
    fun noDemoIsRegisteredTwice() {
        val duplicates = demos
            .groupBy { it.kClass }
            .filterValues { it.size > 1 }
            .keys
            .map { it.simpleName }

        assertThat(duplicates).isEmpty()
    }
}
