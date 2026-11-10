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

package com.google.maps.android.compose.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.android.libraries.navigation.ListenableResultFuture
import com.google.android.libraries.navigation.NavigationView
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.RoutingOptions
import com.google.android.libraries.navigation.Simulator
import com.google.android.libraries.navigation.Waypoint
import com.google.common.truth.Truth.assertThat
import com.google.maps.android.compose.navigation.shadows.ShadowNavBitmapDescriptorFactory
import com.google.maps.android.compose.navigation.shadows.ShadowNavCameraUpdateFactory
import com.google.maps.android.compose.navigation.shadows.ShadowNavGoogleMap
import com.google.maps.android.compose.navigation.shadows.ShadowNavigationApi
import com.google.maps.android.compose.navigation.shadows.ShadowNavigationView
import com.google.maps.android.compose.navigation.shadows.findNavigationView
import com.google.maps.android.robolectric.annotation.EnableMapsShadows
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    shadows = [
        ShadowNavigationView::class,
        ShadowNavigationApi::class,
        ShadowNavGoogleMap::class,
        ShadowNavCameraUpdateFactory::class,
        ShadowNavBitmapDescriptorFactory::class,
    ],
)
@EnableMapsShadows
class MainActivityTest {

    @get:Rule
    val composeTestRule: ComposeTestRule = createEmptyComposeRule()

    @Before
    fun injectMockPlacesClient() {
        ShadowNavigationView.resetInstances()
        ShadowNavigationApi.reset()
        val app = ApplicationProvider.getApplicationContext<NavigationApplication>()
        app.customPlacesClient = mockk(relaxed = true)
    }

    @Test
    fun mainActivity_registersNavigator_mountsNavigationView_andSupportsStopAndSpeedControls() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            composeTestRule.waitForIdle()

            // Verify MainActivity registered its NavigationViewModel with NavigationApi.getNavigator
            val listener = ShadowNavigationApi.lastListener
            assertThat(listener).isNotNull()

            // Verify NavigationView is mounted inside MainActivity's Compose hierarchy
            var navigationView: NavigationView? = null
            scenario.onActivity { activity ->
                navigationView = activity.window.decorView.findNavigationView()
            }
            assertThat(navigationView).isNotNull()

            val shadowNavView = Shadow.extract(navigationView) as ShadowNavigationView
            val googleMap = shadowNavView.googleMap
            assertThat(googleMap).isNotNull()

            // Simulate NavigationApi delivering the Navigator and firing RouteChangedListener
            val routeChangedSlot = slot<Navigator.RouteChangedListener>()
            val simulator = mockk<Simulator>(relaxed = true)
            val navigator = mockk<Navigator>(relaxed = true) {
                every { this@mockk.simulator } returns simulator
                every { addRouteChangedListener(capture(routeChangedSlot)) } answers {}
                every {
                    setDestination(any<Waypoint>(), any<RoutingOptions>())
                } returns mockk<ListenableResultFuture<Navigator.RouteStatus>>(relaxed = true)
            }

            scenario.onActivity {
                listener!!.onNavigatorReady(navigator)
            }
            composeTestRule.waitForIdle()

            // Verify the "navigator ready" Snackbar is displayed in MainActivity
            composeTestRule.onNodeWithText("navigator ready").assertIsDisplayed()

            // Trigger route changed -> Stop button appears in the collapsible controls
            scenario.onActivity {
                routeChangedSlot.captured.onRouteChanged()
            }
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Stop").assertIsDisplayed().performClick()
            composeTestRule.waitForIdle()

            verify { simulator.unsetUserLocation() }
            verify { navigator.stopGuidance() }
            verify { navigator.clearDestinations() }
        }
    }
}
