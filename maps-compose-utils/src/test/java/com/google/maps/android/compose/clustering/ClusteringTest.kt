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

package com.google.maps.android.compose.clustering

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.maps.robolectric.annotation.EnableMapsShadows
import com.google.android.maps.robolectric.shadows.ShadowGoogleMap
import com.google.common.truth.Truth.assertThat
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.ClusterRenderer
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapsComposeExperimentalApi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow

// [START maps_compose_utils_clustering_test]
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@EnableMapsShadows
@OptIn(MapsComposeExperimentalApi::class)
internal class ClusteringTest {

    @get:Rule
    internal val composeTestRule: AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity> =
        createAndroidComposeRule<ComponentActivity>()

    private data class SampleClusterItem(
        override val position: LatLng,
        override val title: String? = null,
        override val snippet: String? = null,
        override val zIndex: Float? = null,
    ) : ClusterItem

    @Test
    internal fun testClusteringMarkerProperties_updatesCompositionLocal(): Unit {
        val properties = ClusteringMarkerProperties()
        assertThat(properties.anchor).isNull()
        assertThat(properties.zIndex).isNull()
        assertThat(properties.rotation).isNull()

        composeTestRule.setContent {
            CompositionLocalProvider(LocalClusteringMarkerProperties provides properties) {
                ClusteringMarkerProperties(
                    anchor = Offset(0.5f, 0.5f),
                    zIndex = 3.0f,
                    rotation = 45f
                )
            }
        }
        composeTestRule.waitForIdle()

        assertThat(properties.anchor).isEqualTo(Offset(0.5f, 0.5f))
        assertThat(properties.zIndex).isEqualTo(3.0f)
        assertThat(properties.rotation).isEqualTo(45f)
    }

    @Test
    internal fun testClustering_defaultRenderer_rendersItemsAndClusters(): Unit {
        var underlyingMap: GoogleMap? = null
        var managerFromCallback: ClusterManager<SampleClusterItem>? = null
        val items = listOf(
            SampleClusterItem(LatLng(37.7749, -122.4194), "SF 1"),
            SampleClusterItem(LatLng(37.7750, -122.4195), "SF 2"),
            SampleClusterItem(LatLng(40.7128, -74.0060), "NYC")
        )

        composeTestRule.setContent {
            GoogleMap {
                MapEffect(Unit) { map ->
                    underlyingMap = map
                }
                Clustering(
                    items = items,
                    onClusterManager = { manager ->
                        managerFromCallback = manager
                    }
                )
            }
        }

        composeTestRule.waitForIdle()

        assertThat(underlyingMap).isNotNull()
        val shadowMap = Shadow.extract(underlyingMap) as ShadowGoogleMap
        assertThat(managerFromCallback).isNotNull()

        // ClusterManager should have added markers to the map
        assertThat(shadowMap.markers.isNotEmpty()).isTrue()
    }

    @Test
    internal fun testClustering_withCustomComposeContent(): Unit {
        var decoratedItem: SampleClusterItem? = null
        val items = listOf(
            SampleClusterItem(LatLng(37.7749, -122.4194), "SF Point")
        )

        composeTestRule.setContent {
            GoogleMap {
                Clustering(
                    items = items,
                    clusterContent = { cluster ->
                        ClusteringMarkerProperties(rotation = 10f)
                    },
                    clusterItemContent = { item ->
                        ClusteringMarkerProperties(rotation = 20f)
                    },
                    clusterItemDecoration = { item ->
                        decoratedItem = item
                    }
                )
            }
        }

        composeTestRule.waitForIdle()
        assertThat(decoratedItem).isEqualTo(items.first())
    }

    @Test
    internal fun testClustering_dynamicItemUpdates(): Unit {
        var itemsState by mutableStateOf(
            listOf(SampleClusterItem(LatLng(10.0, 10.0), "Initial"))
        )
        var clusterManager: ClusterManager<SampleClusterItem>? = null

        composeTestRule.setContent {
            GoogleMap {
                Clustering(
                    items = itemsState,
                    onClusterManager = { clusterManager = it }
                )
            }
        }

        composeTestRule.waitForIdle()
        assertThat(clusterManager).isNotNull()
        assertThat(clusterManager!!.algorithm.items).hasSize(1)

        // Update items list
        itemsState = listOf(
            SampleClusterItem(LatLng(20.0, 20.0), "Updated 1"),
            SampleClusterItem(LatLng(20.1, 20.1), "Updated 2")
        )
        composeTestRule.waitForIdle()

        assertThat(clusterManager.algorithm.items).hasSize(2)
    }

    @Test
    internal fun testRememberClusterManagerAndRenderer_nonNullWhenMapReady(): Unit {
        var rememberedManager: ClusterManager<SampleClusterItem>? = null
        var rememberedRenderer: ClusterRenderer<SampleClusterItem>? = null

        composeTestRule.setContent {
            GoogleMap {
                val manager = rememberClusterManager<SampleClusterItem>()
                val renderer = rememberClusterRenderer(manager)
                rememberedManager = manager
                rememberedRenderer = renderer
            }
        }

        composeTestRule.waitForIdle()

        assertThat(rememberedManager).isNotNull()
        assertThat(rememberedRenderer).isNotNull()
    }
}
// [END maps_compose_utils_clustering_test]
