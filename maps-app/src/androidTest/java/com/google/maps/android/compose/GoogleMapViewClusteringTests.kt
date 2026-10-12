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
import androidx.compose.runtime.mutableStateOf
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
        assumeValidApiKey()

        composeTestRule.setContent {
            GoogleMapView(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
            ) {
                content()
            }
        }

        // Wait for the clustered marker itself rather than for onMapLoaded. Markers are added once
        // the map is ready, while onMapLoaded also waits for every tile to render, which can take
        // longer than the timeout on a slow CI emulator and made these tests flaky.
        val timeoutMillis = TimeUnit.SECONDS.toMillis(MAP_LOAD_TIMEOUT_SECONDS)
        composeTestRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeTestRule.runOnUiThread {
                val cm = clusterManagerHolder[0]
                cm != null && cm.markerCollection.getMarkers().isNotEmpty()
            }
        }

        return composeTestRule.runOnUiThread {
            clusterManagerHolder[0]!!.markerCollection.getMarkers().first()
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

    @OptIn(MapsComposeExperimentalApi::class)
    @Test
    fun testKeyedClusteringAppliesAdditionsRemovalsAndUpdates() {
        val clusterManagerHolder = arrayOfNulls<ClusterManager<MyItem>>(1)
        val firstItem = MyItem(startingPosition, "first", "Snippet", 0f)
        val items = mutableStateOf(listOf(firstItem))

        composeTestRule.setContent {
            GoogleMapView(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
            ) {
                Clustering(
                    items = items.value,
                    key = MyItem::title,
                    onClusterManager = { clusterManagerHolder[0] = it },
                )
            }
        }

        val timeoutMillis = TimeUnit.SECONDS.toMillis(MAP_LOAD_TIMEOUT_SECONDS)
        composeTestRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeTestRule.runOnUiThread {
                clusterManagerHolder[0]?.algorithm?.items?.size == 1
            }
        }

        val updatedPosition = LatLng(2.34, 5.67)
        val addedItem = MyItem(LatLng(3.45, 6.78), "added", "Snippet", 0f)
        items.value = listOf(firstItem.copy(position = updatedPosition), addedItem)
        composeTestRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeTestRule.runOnUiThread {
                val currentItems = clusterManagerHolder[0]?.algorithm?.items.orEmpty()
                currentItems.size == 2 &&
                    currentItems.any { it.title == "first" && it.position == updatedPosition } &&
                    currentItems.any { it.title == "added" }
            }
        }

        items.value = listOf(addedItem)
        composeTestRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeTestRule.runOnUiThread {
                val currentItems = clusterManagerHolder[0]?.algorithm?.items.orEmpty()
                currentItems.size == 1 && currentItems.single().title == "added"
            }
        }
    }

    @OptIn(MapsComposeExperimentalApi::class)
    @Test
    fun testKeyedClusteringLeavesUnchangedItemsAlone() {
        val clusterManagerHolder = arrayOfNulls<ClusterManager<MyItem>>(1)
        val initialItem = MyItem(startingPosition, "same", "Snippet", 0f)
        val items = mutableStateOf(listOf(initialItem))

        composeTestRule.setContent {
            GoogleMapView(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
            ) {
                Clustering(
                    items = items.value,
                    key = MyItem::title,
                    onClusterManager = { clusterManagerHolder[0] = it },
                )
            }
        }

        val timeoutMillis = TimeUnit.SECONDS.toMillis(MAP_LOAD_TIMEOUT_SECONDS)
        composeTestRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeTestRule.runOnUiThread {
                clusterManagerHolder[0]?.algorithm?.items?.size == 1
            }
        }
        val marker = composeTestRule.runOnUiThread {
            clusterManagerHolder[0]!!.markerCollection.getMarkers().single()
        }

        items.value = listOf(initialItem.copy())
        composeTestRule.waitForIdle()

        composeTestRule.runOnUiThread {
            assertThat(clusterManagerHolder[0]?.algorithm?.items).containsExactly(initialItem)
            assertThat(clusterManagerHolder[0]?.markerCollection?.getMarkers()).contains(marker)
        }
    }

    @OptIn(MapsComposeExperimentalApi::class)
    @Test
    fun testUnkeyedClusteringStillUpdatesItemsByEquality() {
        val clusterManagerHolder = arrayOfNulls<ClusterManager<MyItem>>(1)
        val initialItem = MyItem(startingPosition, "first", "Snippet", 0f)
        val items = mutableStateOf(listOf(initialItem))

        composeTestRule.setContent {
            GoogleMapView(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
            ) {
                Clustering(
                    items = items.value,
                    onClusterManager = { clusterManagerHolder[0] = it },
                )
            }
        }

        val timeoutMillis = TimeUnit.SECONDS.toMillis(MAP_LOAD_TIMEOUT_SECONDS)
        composeTestRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeTestRule.runOnUiThread {
                clusterManagerHolder[0]?.algorithm?.items?.size == 1
            }
        }

        val updatedItem = initialItem.copy(position = LatLng(2.34, 5.67))
        val addedItem = MyItem(LatLng(3.45, 6.78), "added", "Snippet", 0f)
        items.value = listOf(updatedItem, addedItem)
        composeTestRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeTestRule.runOnUiThread {
                val currentItems = clusterManagerHolder[0]?.algorithm?.items.orEmpty()
                currentItems.size == 2 &&
                    currentItems.any { it.title == "first" && it.position == updatedItem.position } &&
                    currentItems.any { it.title == "added" }
            }
        }
    }

    @OptIn(MapsComposeExperimentalApi::class)
    @Test
    fun testClusterItemContentUsingRememberComposeBitmapDescriptorDoesNotCrash() {
        val clusterManagerHolder = arrayOfNulls<ClusterManager<MyItem>>(1)
        val items = listOf(MyItem(startingPosition, "Item", "Snippet", 0f))

        // Regression test for https://github.com/googlemaps/android-maps-compose/issues/694:
        // rememberComposeBitmapDescriptor used to throw "measured to have a width or height of
        // zero" when called from content composed inside Clustering's clusterItemContent,
        // because its parent (InvalidatingComposeView) hadn't been through an Android layout
        // pass yet at that point.
        val marker = initMapAndGetMarker(clusterManagerHolder) {
            Clustering(
                items = items,
                clusterItemContent = {
                    rememberComposeBitmapDescriptor {
                        Surface(modifier = Modifier.size(20.dp)) {
                            Text("X")
                        }
                    }
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
            assertThat(marker.isVisible).isTrue()
        }
    }
}
