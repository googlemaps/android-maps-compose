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
import com.google.android.gms.maps.model.MarkerOptions
import com.google.common.truth.Truth.assertThat
import com.google.maps.android.clustering.ClusterItem
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

    /**
     * Inspects non-null String fields on [MarkerOptions] to extract contentDescription
     * dynamically without hardcoding obfuscated Play services field names.
     */
    private fun MarkerOptions.findCustomContentDescription(): String? {
        for (field in MarkerOptions::class.java.declaredFields) {
            if (field.type == String::class.java) {
                field.isAccessible = true
                val value = field.get(this) as? String
                if (value != null && value != this.title && value != this.snippet) {
                    return value
                }
            }
        }
        return null
    }

    private fun MarkerOptions.countStringFieldOccurrences(target: String): Int {
        var count = 0
        for (field in MarkerOptions::class.java.declaredFields) {
            if (field.type == String::class.java) {
                field.isAccessible = true
                if (field.get(this) == target) {
                    count++
                }
            }
        }
        return count
    }

    @OptIn(MapsComposeExperimentalApi::class)
    @Test
    fun testClusteringPropagatesItemTitleToMarkerContentDescription() {
        val clusterManagerHolder = arrayOfNulls<ClusterManager<MyItem>>(1)
        val items = listOf(MyItem(startingPosition, "Store Location 42", "Snippet", 0f))

        initMapAndGetMarker(clusterManagerHolder) {
            Clustering<MyItem>(
                items = items,
                onClusterManager = { cm ->
                    clusterManagerHolder[0] = cm
                }
            )
        }

        composeTestRule.runOnUiThread {
            val cm = clusterManagerHolder[0]!!
            val renderer = cm.renderer
            val method = renderer.javaClass.methods.firstOrNull {
                it.name == "onBeforeClusterItemRendered" && it.parameterTypes.size == 2
            } ?: renderer.javaClass.getDeclaredMethod(
                "onBeforeClusterItemRendered",
                ClusterItem::class.java,
                MarkerOptions::class.java
            ).apply { isAccessible = true }

            val markerOptions = MarkerOptions()
            method.invoke(renderer, items.first(), markerOptions)

            // When no custom description is supplied, fallback to item.title is populated on MarkerOptions.
            // Both title and contentDescription fields hold "Store Location 42" (count == 2),
            // whereas before the fix contentDescription remained null (count == 1).
            assertThat(markerOptions.countStringFieldOccurrences("Store Location 42")).isEqualTo(2)
        }
    }

    @OptIn(MapsComposeExperimentalApi::class)
    @Test
    fun testClusteringMarkerPropertiesCustomContentDescription() {
        val clusterManagerHolder = arrayOfNulls<ClusterManager<MyItem>>(1)
        // Item title is deliberately distinct from the custom contentDescription
        val items = listOf(MyItem(startingPosition, "Store Location 42", "Snippet", 0f))

        initMapAndGetMarker(clusterManagerHolder) {
            Clustering<MyItem>(
                items = items,
                clusterItemContent = {
                    ClusteringMarkerProperties(
                        contentDescription = "Custom Accessibility Pin Description"
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
            val cm = clusterManagerHolder[0]!!
            val renderer = cm.renderer
            val method = renderer.javaClass.methods.firstOrNull {
                it.name == "onBeforeClusterItemRendered" && it.parameterTypes.size == 2
            } ?: renderer.javaClass.getDeclaredMethod(
                "onBeforeClusterItemRendered",
                ClusterItem::class.java,
                MarkerOptions::class.java
            ).apply { isAccessible = true }

            val markerOptions = MarkerOptions()
            method.invoke(renderer, items.first(), markerOptions)

            assertThat(markerOptions.findCustomContentDescription()).isEqualTo("Custom Accessibility Pin Description")
        }
    }
}
