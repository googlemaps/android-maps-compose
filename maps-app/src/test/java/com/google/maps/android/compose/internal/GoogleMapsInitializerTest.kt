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
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.MapsApiSettings
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class GoogleMapsInitializerTest {

    private val mockContext: Context = mockk(relaxed = true)
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var googleMapsInitializer: GoogleMapsInitializer

    @Before
    fun setUp() {
        // Mock the static methods we depend on
        mockkStatic(MapsInitializer::class)
        mockkStatic(MapsApiSettings::class)

        // Default happy path behavior for mocks
        every { MapsInitializer.initialize(any()) } returns ConnectionResult.SUCCESS
        every { MapsApiSettings.addInternalUsageAttributionId(any(), any()) } returns Unit

        testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        googleMapsInitializer = DefaultGoogleMapsInitializer(testDispatcher)
    }

    @Test
    fun `initialize - when coroutine is cancelled - state resets to UNINITIALIZED`() = runTest {
        val job = launch {
            googleMapsInitializer.initialize(mockContext)
        }
        job.cancel()
        assertEquals(InitializationState.UNINITIALIZED, googleMapsInitializer.state.value)
    }

    @Test
    fun `initialize - on recoverable failure - state resets to UNINITIALIZED and exception is thrown`() = runTest {
        // Arrange
        val error = RuntimeException("A network error occurred!")
        every { MapsInitializer.initialize(any()) } throws error

        // Act & Assert
        assertFailsWith<RuntimeException> {
            googleMapsInitializer.initialize(mockContext)
        }
        assertEquals(InitializationState.UNINITIALIZED, googleMapsInitializer.state.value)
    }
}
