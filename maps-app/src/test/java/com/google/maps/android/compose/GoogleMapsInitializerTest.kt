// Copyright 2025 Google LLC
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

package com.google.maps.android.compose

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.maps.android.compose.internal.GoogleMapsInitializer
import com.google.maps.android.compose.internal.InitializationState
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GoogleMapsInitializerTest {

    @After
    fun tearDown() {
        GoogleMapsInitializer.reset()
    }

    @Test
    fun testInitializationFailure() {
        // In a unit test environment, Google Play services are not available.
        // Therefore, we expect the initialization to fail.
        val context: Context = ApplicationProvider.getApplicationContext()

        GoogleMapsInitializer.initialize(context)

        // Wait for the initialization to complete
        Thread.sleep(1000)

        assertThat(GoogleMapsInitializer.state.value).isEqualTo(InitializationState.FAILURE)
    }
}