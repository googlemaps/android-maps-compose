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
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesMissingManifestValueException
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.MapsApiSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkStatic
import io.mockk.Runs
import io.mockk.just

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

    @Test
    fun testInitializeSuccessState() = runTest {
        // Arrange
        mockkStatic(MapsInitializer::class)
        assertThat(googleMapsInitializer.state.value).isEqualTo(InitializationState.UNINITIALIZED)

        coEvery { MapsInitializer.initialize(any()) } returns ConnectionResult.SUCCESS

        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        // Act
        // Direct call pattern matching original successful test structure
        googleMapsInitializer.initialize(context)

        // Assert
        assertThat(googleMapsInitializer.state.value).isEqualTo(InitializationState.SUCCESS)
        coVerify(exactly = 1) {  MapsInitializer.initialize(
            eq(context),
            any(),
            any(),
        )}
    }

    @Test
    fun testInitializeConcurrentCallsOnlyRunOnce() = runTest {
        mockkStatic(MapsInitializer::class)
        coEvery { MapsInitializer.initialize(any()) } returns ConnectionResult.SUCCESS

        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val job1 = launch { googleMapsInitializer.initialize(context) }
        val job2 = launch { googleMapsInitializer.initialize(context) }

        job1.join()
        job2.join()

        // Assert: The actual initialization method should only have been called once
        coVerify(exactly = 1) {  MapsInitializer.initialize(
            eq(context),
            any(),
            any(),
        )}
        assertThat(googleMapsInitializer.state.value).isEqualTo(InitializationState.SUCCESS)
    }

    @Test
    fun testInitializeUnrecoverableFailureSetsFailureState() = runTest {
        // Arrange
        mockkStatic(MapsInitializer::class)
        val error = GooglePlayServicesMissingManifestValueException()

        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        var caughtException: Throwable? = null

        coEvery {
            MapsInitializer.initialize(
                eq(context),
                isNull(),
                any()
            )
        } throws error

        // Act
        val job = launch {
            try {
                googleMapsInitializer.initialize(context)
            } catch (e: GooglePlayServicesMissingManifestValueException) {
                caughtException = e
            }
        }
        job.join()

        // Assert: The exception was caught, and the state became FAILURE
        assertThat(caughtException).isInstanceOf(GooglePlayServicesMissingManifestValueException::class.java)
        assertThat(caughtException).isEqualTo(error)

        // 2. Assert the state was set to FAILURE
        assertThat(googleMapsInitializer.state.value).isEqualTo(InitializationState.FAILURE)
    }

    @Test
    fun testInitializeSuccessAlsoSetsAttributionId() = runTest {
        // Arrange: Mock MapsApiSettings locally
        mockkStatic(MapsInitializer::class, MapsApiSettings::class)

        coEvery { MapsInitializer.initialize(any()) } returns ConnectionResult.SUCCESS
        coEvery { MapsApiSettings.addInternalUsageAttributionId(any(), any()) } just Runs

        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

        // Act
        // Direct call pattern matching original successful test structure
        googleMapsInitializer.initialize(context)

        // Assert: Verify both the primary initialization and the attribution call occurred
        coVerify(exactly = 1) {
            MapsInitializer.initialize(
                eq(context),
                any(),
                any(),
            )
        }
        coVerify(exactly = 1) { MapsApiSettings.addInternalUsageAttributionId(any(), any()) }
        assertThat(googleMapsInitializer.state.value).isEqualTo(InitializationState.SUCCESS)
    }
}