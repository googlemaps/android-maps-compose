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

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.maps.robolectric.annotation.EnableMapsShadows
import com.google.android.maps.robolectric.shadows.ShadowGoogleMap
import com.google.android.maps.robolectric.shadows.ShadowMapView
import com.google.android.maps.robolectric.truth.LatLngSubject
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow

// [START maps_compose_google_map_composable_test]
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@EnableMapsShadows
@OptIn(MapsComposeExperimentalApi::class)
internal class GoogleMapTest {

    @get:Rule
    internal val composeTestRule: AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity> =
        createAndroidComposeRule<ComponentActivity>()

    private val sydney: LatLng = LatLng(-33.852, 151.211)

    @Test
    internal fun testGoogleMap_rendersWithMarkersAndCircles() {
        lateinit var cameraState: CameraPositionState
        var underlyingMap: GoogleMap? = null

        composeTestRule.setContent {
            cameraState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(sydney, 11f)
            }
            GoogleMap(
                cameraPositionState = cameraState,
                modifier = Modifier.fillMaxSize(),
            ) {
                MapEffect(Unit) { map ->
                    underlyingMap = map
                }
                Marker(
                    state = rememberUpdatedMarkerState(position = sydney),
                    title = "Sydney Flagship",
                )
                Circle(
                    center = sydney,
                    radius = 1500.0,
                )
            }
        }

        composeTestRule.waitForIdle()

        assertThat(underlyingMap).isNotNull()
        val shadowMap = Shadow.extract(underlyingMap) as ShadowGoogleMap

        // Verify camera position
        LatLngSubject.assertThat(cameraState.position.target)
            .isWithin(1.0)
            .of(sydney)
        assertThat(cameraState.position.zoom).isEqualTo(11f)

        // Verify Marker rendered via MapApplier
        assertThat(shadowMap.markers).hasSize(1)
        val marker = shadowMap.markers.first()
        assertThat(marker.title).isEqualTo("Sydney Flagship")
        LatLngSubject.assertThat(marker.position)
            .isWithin(1.0)
            .of(sydney)

        // Verify Circle rendered via MapApplier
        assertThat(shadowMap.circles).hasSize(1)
        val circle = shadowMap.circles.first()
        assertThat(circle.radius).isEqualTo(1500.0)
        LatLngSubject.assertThat(circle.center)
            .isWithin(1.0)
            .of(sydney)
    }

    @Test
    internal fun testGoogleMap_markerClickUpdatesComposeState() {
        var underlyingMap: GoogleMap? = null
        var markerClicked by mutableStateOf(false)

        composeTestRule.setContent {
            val cameraState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(sydney, 10f)
            }
            GoogleMap(
                cameraPositionState = cameraState,
                modifier = Modifier.fillMaxSize(),
            ) {
                MapEffect(Unit) { map ->
                    underlyingMap = map
                }
                Marker(
                    state = rememberUpdatedMarkerState(position = sydney),
                    title = "Clickable Marker",
                    onClick = {
                        markerClicked = true
                        true
                    },
                )
            }
        }

        composeTestRule.waitForIdle()

        assertThat(underlyingMap).isNotNull()
        val shadowMap = Shadow.extract(underlyingMap) as ShadowGoogleMap
        val marker = shadowMap.markers.first()

        // Simulate user clicking on marker
        shadowMap.clickMarker(marker)
        composeTestRule.waitForIdle()

        assertThat(markerClicked).isTrue()
    }

    @Test
    internal fun testGoogleMap_dynamicRecompositionAddsAndRemovesMarker() {
        var underlyingMap: GoogleMap? = null
        var showMarker by mutableStateOf(true)

        composeTestRule.setContent {
            val cameraState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(sydney, 10f)
            }
            GoogleMap(
                cameraPositionState = cameraState,
                modifier = Modifier.fillMaxSize(),
            ) {
                MapEffect(Unit) { map ->
                    underlyingMap = map
                }
                if (showMarker) {
                    Marker(
                        state = rememberUpdatedMarkerState(position = sydney),
                        title = "Conditional Marker",
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        assertThat(underlyingMap).isNotNull()
        val shadowMap = Shadow.extract(underlyingMap) as ShadowGoogleMap
        assertThat(shadowMap.markers).hasSize(1)

        // Mutate Compose state to remove marker
        showMarker = false
        composeTestRule.waitForIdle()

        // Assert that the marker was dynamically removed from the map
        assertThat(shadowMap.markers).isEmpty()
    }

    @Test
    internal fun testGoogleMap_customMapViewCreatorDelegate_lifecycleAndMarkerComposable() {
        val lifecycleEvents = mutableListOf<String>()
        val fakeGoogleMap = ShadowMapView.createFakeGoogleMap()
        var customDelegate: AbstractMapViewDelegate<FrameLayout>? = null

        composeTestRule.setContent {
            val cameraState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(sydney, 13f)
            }
            GoogleMap(
                cameraPositionState = cameraState,
                modifier = Modifier.fillMaxSize(),
                mapViewCreator = { context, _ ->
                    object : AbstractMapViewDelegate<FrameLayout> {
                        override val mapView: FrameLayout = FrameLayout(context)
                        override fun onCreate(savedInstanceState: Bundle?) {
                            lifecycleEvents.add("onCreate")
                        }
                        override fun onStart() {
                            lifecycleEvents.add("onStart")
                        }
                        override fun onResume() {
                            lifecycleEvents.add("onResume")
                        }
                        override fun onPause() {
                            lifecycleEvents.add("onPause")
                        }
                        override fun onStop() {
                            lifecycleEvents.add("onStop")
                        }
                        override fun onLowMemory() {
                            lifecycleEvents.add("onLowMemory")
                        }
                        override fun onDestroy() {
                            lifecycleEvents.add("onDestroy")
                        }
                        override suspend fun awaitMap(): GoogleMap = fakeGoogleMap
                    }.also { customDelegate = it }
                },
            ) {
                Marker(
                    state = rememberUpdatedMarkerState(position = sydney),
                    title = "Delegated Marker",
                )
                MarkerComposable(
                    state = rememberUpdatedMarkerState(position = sydney),
                    title = "Composable Marker",
                ) {
                    Box(modifier = Modifier.size(32.dp).background(Color.Blue))
                }
            }
        }

        composeTestRule.waitForIdle()

        assertThat(customDelegate).isNotNull()
        assertThat(lifecycleEvents).containsExactly("onCreate", "onStart", "onResume").inOrder()

        val shadowMap = Shadow.extract(fakeGoogleMap) as ShadowGoogleMap
        assertThat(shadowMap.markers).hasSize(2)
        assertThat(shadowMap.markers.map { it.title }).containsExactly("Delegated Marker", "Composable Marker")
    }
}
// [END maps_compose_google_map_composable_test]
