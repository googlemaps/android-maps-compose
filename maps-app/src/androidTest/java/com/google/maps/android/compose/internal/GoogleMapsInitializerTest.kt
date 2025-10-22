// Copyright 2025 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.maps.android.compose.internal

import android.content.Context
import android.os.StrictMode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class GoogleMapsInitializerTest {

    private val googleMapsInitializer = DefaultGoogleMapsInitializer()

    @After
    fun tearDown() = runTest {
        googleMapsInitializer.reset()
    }

    @Test
    fun testInitializationSuccess() = runTest {
        // In an instrumentation test environment, Google Play services are available.
        // Therefore, we expect the initialization to succeed.

        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

        // Note: we need to establish the Strict Mode settings here as there are violations outside
        // of our control if we try to set them in setUp
        val threadPolicy = StrictMode.getThreadPolicy()
        val vmPolicy = StrictMode.getVmPolicy()

        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectAll()
                .penaltyLog()
                .penaltyDeath()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build()
        )

        googleMapsInitializer.initialize(context)

        StrictMode.setThreadPolicy(threadPolicy)
        StrictMode.setVmPolicy(vmPolicy)

        assertThat(googleMapsInitializer.state.value).isEqualTo(InitializationState.SUCCESS)
    }

    @Test
    fun testInitializationCancellationLeavesStateUninitialized() = runTest {
        // In an instrumentation test environment, Google Play services are available.
        // Therefore, we expect the initialization to succeed.

        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

        // Note: we need to establish the Strict Mode settings here as there are violations outside
        // of our control if we try to set them in setUp
        val threadPolicy = StrictMode.getThreadPolicy()
        val vmPolicy = StrictMode.getVmPolicy()

        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectAll()
                .penaltyLog()
                .penaltyDeath()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build()
        )

        val job = launch {
            googleMapsInitializer.reset()
            googleMapsInitializer.initialize(context)
        }

        // Allow the initialization coroutine to start before we cancel it.
        delay(1.milliseconds)
        job.cancel()
        job.join()

        StrictMode.setThreadPolicy(threadPolicy)
        StrictMode.setVmPolicy(vmPolicy)

        assertThat(googleMapsInitializer.state.value).isEqualTo(InitializationState.UNINITIALIZED)
    }
}