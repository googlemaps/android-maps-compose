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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.ClusterRenderer
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.coroutines.EmptyCoroutineContext

@RunWith(RobolectricTestRunner::class)
public class ClusterInfoWindowContentTest {

    private data class TestItem(
        override val position: LatLng = LatLng(1.0, 2.0),
        override val title: String? = null,
        override val snippet: String? = null,
        override val zIndex: Float? = null,
    ) : ClusterItem

    private val clusterMarker = mockk<Marker>()
    private val itemMarker = mockk<Marker>()
    private val foreignMarker = mockk<Marker>()
    private val cluster = mockk<Cluster<TestItem>>()
    private val item = TestItem()
    private val renderer = mockk<DefaultClusterRenderer<TestItem>>()

    private val composedClusters = mutableListOf<Cluster<TestItem>>()
    private val composedItems = mutableListOf<TestItem>()
    private val clusterContent: @Composable (Cluster<TestItem>) -> Unit = { composedClusters += it }
    private val clusterItemContent: @Composable (TestItem) -> Unit = { composedItems += it }

    private val clusterManager = mockk<ClusterManager<TestItem>>()
    private val windowClusters = mutableListOf<Cluster<TestItem>>()
    private val contentClusters = mutableListOf<Cluster<TestItem>>()
    private val windowItems = mutableListOf<TestItem>()
    private val contentItems = mutableListOf<TestItem>()
    private val clusterInfoWindow: @Composable (Cluster<TestItem>) -> Unit = { windowClusters += it }
    private val clusterInfoWindowContent: @Composable (Cluster<TestItem>) -> Unit = { contentClusters += it }
    private val clusterItemInfoWindow: @Composable (TestItem) -> Unit = { windowItems += it }
    private val clusterItemInfoWindowContent: @Composable (TestItem) -> Unit = { contentItems += it }

    @Before
    public fun setUp() {
        every { renderer.getCluster(any()) } returns null
        every { renderer.getCluster(clusterMarker) } returns cluster
        every { renderer.getClusterItem(any()) } returns null
        every { renderer.getClusterItem(itemMarker) } returns item
        every { clusterManager.renderer } returns renderer
    }

    @Test
    public fun testClusterMarkerGetsClusterContentBoundToThatCluster() {
        val content = renderer.infoWindowContent(clusterMarker, clusterContent, clusterItemContent)

        composeOnce(checkNotNull(content))

        assertEquals(listOf(cluster), composedClusters)
        assertEquals(emptyList<TestItem>(), composedItems)
    }

    @Test
    public fun testItemMarkerGetsItemContentBoundToThatItem() {
        val content = renderer.infoWindowContent(itemMarker, clusterContent, clusterItemContent)

        composeOnce(checkNotNull(content))

        assertEquals(listOf(item), composedItems)
        assertEquals(emptyList<Cluster<TestItem>>(), composedClusters)
    }

    @Test
    public fun testMarkerGetsNoContentWhenThereIsNoneForItsKind() {
        assertNull(renderer.infoWindowContent(clusterMarker, null, clusterItemContent))
        assertNull(renderer.infoWindowContent(itemMarker, clusterContent, null))
    }

    @Test
    public fun testMarkerTheRendererDidNotDrawGetsNoContent() {
        assertNull(renderer.infoWindowContent(foreignMarker, clusterContent, clusterItemContent))
    }

    @Test
    public fun testRendererThatCannotMapMarkersBackToClustersGivesNoContent() {
        val otherRenderer = mockk<ClusterRenderer<TestItem>>()

        assertNull(otherRenderer.infoWindowContent(clusterMarker, clusterContent, clusterItemContent))
    }

    @Test
    public fun testNoLookupsWithoutInfoWindowContent() {
        assertNull(clusterManager.infoWindowLookups(null, null, null, null))
    }

    @Test
    public fun testAnyOneKindOfInfoWindowContentGivesLookups() {
        assertNotNull(clusterManager.infoWindowLookups(clusterInfoWindow, null, null, null))
        assertNotNull(clusterManager.infoWindowLookups(null, clusterInfoWindowContent, null, null))
        assertNotNull(clusterManager.infoWindowLookups(null, null, clusterItemInfoWindow, null))
        assertNotNull(clusterManager.infoWindowLookups(null, null, null, clusterItemInfoWindowContent))
    }

    @Test
    public fun testInfoWindowLookupGivesWholeWindowContent() {
        val lookups = checkNotNull(allInfoWindowLookups())

        composeOnce(checkNotNull(lookups.infoWindow(clusterMarker)))
        composeOnce(checkNotNull(lookups.infoWindow(itemMarker)))

        assertEquals(listOf(cluster), windowClusters)
        assertEquals(listOf(item), windowItems)
        assertEquals(emptyList<Cluster<TestItem>>(), contentClusters)
        assertEquals(emptyList<TestItem>(), contentItems)
    }

    @Test
    public fun testInfoContentLookupGivesInFrameContent() {
        val lookups = checkNotNull(allInfoWindowLookups())

        composeOnce(checkNotNull(lookups.infoContent(clusterMarker)))
        composeOnce(checkNotNull(lookups.infoContent(itemMarker)))

        assertEquals(listOf(cluster), contentClusters)
        assertEquals(listOf(item), contentItems)
        assertEquals(emptyList<Cluster<TestItem>>(), windowClusters)
        assertEquals(emptyList<TestItem>(), windowItems)
    }

    @Test
    public fun testLookupsGoThroughClusterManagersCurrentRenderer() {
        val previousRenderer = mockk<DefaultClusterRenderer<TestItem>>()
        every { previousRenderer.getCluster(any()) } returns null
        every { previousRenderer.getClusterItem(any()) } returns null
        every { clusterManager.renderer } returns previousRenderer
        val lookups = checkNotNull(allInfoWindowLookups())

        assertNull(lookups.infoWindow(clusterMarker))
        assertNull(lookups.infoContent(clusterMarker))

        every { clusterManager.renderer } returns renderer

        assertNotNull(lookups.infoWindow(clusterMarker))
        assertNotNull(lookups.infoContent(clusterMarker))
    }

    private fun allInfoWindowLookups() = clusterManager.infoWindowLookups(
        clusterInfoWindow = clusterInfoWindow,
        clusterInfoWindowContent = clusterInfoWindowContent,
        clusterItemInfoWindow = clusterItemInfoWindow,
        clusterItemInfoWindowContent = clusterItemInfoWindowContent,
    )

    private fun composeOnce(content: @Composable () -> Unit) {
        val composition = Composition(UnitApplier(), Recomposer(EmptyCoroutineContext))
        try {
            composition.setContent(content)
        } finally {
            composition.dispose()
        }
    }
}
