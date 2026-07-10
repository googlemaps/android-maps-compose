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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.common.truth.Truth.assertThat
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.clustering.ClusteringMarkerProperties
import com.google.maps.android.compose.markerexamples.MyItem
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class GoogleMapViewClusteringTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val startingPosition = LatLng(1.23, 4.56)
    private lateinit var cameraPositionState: CameraPositionState

    @Before
    fun setUp() {
        cameraPositionState = CameraPositionState(
            position = CameraPosition.fromLatLngZoom(startingPosition, 12f)
        )
    }

    private fun initMapAndGetMarker(
        clusterManagerHolder: Array<ClusterManager<MyItem>?>,
        content: @Composable () -> Unit
    ): Marker {
        check(hasValidApiKey) { "Maps API key not specified" }
        val countDownLatch = CountDownLatch(1)

        composeTestRule.setContent {
            GoogleMapView(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapLoaded = {
                    countDownLatch.countDown()
                }
            ) {
                content()
            }
        }

        val mapLoaded = countDownLatch.await(MAP_LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        assertThat(mapLoaded).isTrue()

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.runOnUiThread {
                val cm = clusterManagerHolder[0]
                cm != null && cm.markerCollection.markers.isNotEmpty()
            }
        }

        return composeTestRule.runOnUiThread {
            clusterManagerHolder[0]!!.markerCollection.markers.first()
        }
    }

    @OptIn(MapsComposeExperimentalApi::class)
    @Test
    fun testClusteringParametersRotationUpdatesMarker() {
        val rotationState = mutableFloatStateOf(45f)
        val clusterManagerHolder = arrayOfNulls<ClusterManager<MyItem>>(1)
        val items = listOf(MyItem(startingPosition, "Item", "Snippet", 0f))

        val marker = initMapAndGetMarker(clusterManagerHolder) {
            Clustering(
                items = items,
                clusterItemContentRotation = rotationState.floatValue,
                clusterItemContentAnchor = Offset(0.5f, 0.5f),
                clusterItemContent = {
                    Surface(modifier = Modifier.size(20.dp)) {
                        Text("X")
                    }
                },
                onClusterManager = { cm ->
                    clusterManagerHolder[0] = cm
                }
            )
        }

        composeTestRule.runOnUiThread {
            assertThat(marker.rotation).isEqualTo(45f)
        }

        rotationState.floatValue = 180f
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.runOnUiThread { marker.rotation == 180f }
        }

        composeTestRule.runOnUiThread {
            assertThat(marker.rotation).isEqualTo(180f)
        }
    }

    @OptIn(MapsComposeExperimentalApi::class)
    @Test
    fun testClusteringMarkerPropertiesRotationUpdatesMarker() {
        val rotationState = mutableFloatStateOf(45f)
        val clusterManagerHolder = arrayOfNulls<ClusterManager<MyItem>>(1)
        val items = listOf(MyItem(startingPosition, "Item", "Snippet", 0f))

        val marker = initMapAndGetMarker(clusterManagerHolder) {
            Clustering(
                items = items,
                clusterItemContentAnchor = Offset(0.5f, 0.5f),
                clusterItemContent = {
                    ClusteringMarkerProperties(
                        rotation = rotationState.floatValue,
                        anchor = Offset(0.5f, 0.5f)
                    )
                    Surface(modifier = Modifier.size(20.dp)) {
                        Text("X")
                    }
                },
                onClusterManager = { cm ->
                    clusterManagerHolder[0] = cm
                }
            )
        }

        composeTestRule.runOnUiThread {
            assertThat(marker.rotation).isEqualTo(45f)
        }

        rotationState.floatValue = 180f
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.runOnUiThread { marker.rotation == 180f }
        }

        composeTestRule.runOnUiThread {
            assertThat(marker.rotation).isEqualTo(180f)
        }
    }
}
