package com.google.maps.android.compose

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.maps.android.compose.internal.GoogleMapsInitializer
import com.google.maps.android.compose.internal.InitializationState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GoogleMapsInitializerTest {

    @After
    fun tearDown() = runTest {
        GoogleMapsInitializer.reset()
    }

    @Test
    fun testInitializationFailure() = runTest {
        // In a unit test environment, Google Play services are not available.
        // Therefore, we expect the initialization to fail.
        val context: Context = ApplicationProvider.getApplicationContext()

        GoogleMapsInitializer.initialize(context)

        // The initialization is now synchronous within the test scope, so we don't need to wait.
        assertThat(GoogleMapsInitializer.state.value).isEqualTo(InitializationState.FAILURE)
    }
}