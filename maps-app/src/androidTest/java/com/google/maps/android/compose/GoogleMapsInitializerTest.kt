package com.google.maps.android.compose

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.maps.android.compose.internal.GoogleMapsInitializer
import com.google.maps.android.compose.internal.InitializationState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class GoogleMapsInitializerTest {

    @After
    fun tearDown() = runTest {
        GoogleMapsInitializer.reset()
    }

    @Test
    fun testInitializationSuccess() = runTest {
        // In an instrumentation test environment, Google Play services are available.
        // Therefore, we expect the initialization to succeed.
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

        GoogleMapsInitializer.initialize(context)

        assertThat(GoogleMapsInitializer.state.value).isEqualTo(InitializationState.SUCCESS)
    }
}