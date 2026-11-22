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

import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.ClusterManager.OnClusterInfoWindowClickListener
import com.google.maps.android.clustering.ClusterManager.OnClusterInfoWindowLongClickListener
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.coroutines.EmptyCoroutineContext

@RunWith(RobolectricTestRunner::class)
public class ClusterInfoWindowClickListenersTest {

    private data class TestItem(
        override val position: LatLng = LatLng(1.0, 2.0),
        override val title: String? = null,
        override val snippet: String? = null,
        override val zIndex: Float? = null,
    ) : ClusterItem

    private val clusterManager = mockk<ClusterManager<TestItem>>(relaxed = true)
    private val cluster = mockk<Cluster<TestItem>>()
    private val composition = Composition(UnitApplier(), Recomposer(EmptyCoroutineContext))

    private var onClick: ((Cluster<TestItem>) -> Unit)? = null
    private var onLongClick: ((Cluster<TestItem>) -> Unit)? = null

    @After
    public fun tearDown() {
        composition.dispose()
    }

    @Test
    public fun testClickCallbackBecomesClusterManagerListener() {
        val listener = slot<OnClusterInfoWindowClickListener<TestItem>>()
        every { clusterManager.setOnClusterInfoWindowClickListener(capture(listener)) } just runs
        val clicked = mutableListOf<Cluster<TestItem>>()
        onClick = { clicked += it }

        compose()
        listener.captured.onClusterInfoWindowClick(cluster)

        assertEquals(listOf(cluster), clicked)
    }

    @Test
    public fun testLongClickCallbackBecomesClusterManagerListener() {
        val listener = slot<OnClusterInfoWindowLongClickListener<TestItem>>()
        every { clusterManager.setOnClusterInfoWindowLongClickListener(capture(listener)) } just runs
        val longClicked = mutableListOf<Cluster<TestItem>>()
        onLongClick = { longClicked += it }

        compose()
        listener.captured.onClusterInfoWindowLongClick(cluster)

        assertEquals(listOf(cluster), longClicked)
    }

    @Test
    public fun testNullCallbacksLeaveClusterManagerListenersAlone() {
        compose()
        composition.dispose()

        verify(exactly = 0) { clusterManager.setOnClusterInfoWindowClickListener(any()) }
        verify(exactly = 0) { clusterManager.setOnClusterInfoWindowLongClickListener(any()) }
    }

    @Test
    public fun testListenersAreClearedWhenTheyLeaveComposition() {
        onClick = {}
        onLongClick = {}

        compose()
        composition.dispose()

        verify { clusterManager.setOnClusterInfoWindowClickListener(null) }
        verify { clusterManager.setOnClusterInfoWindowLongClickListener(null) }
    }

    @Test
    public fun testOnlyTheListenerWhoseCallbackBecomesNullIsCleared() {
        onClick = {}
        onLongClick = {}
        compose()

        onClick = null
        compose()

        verify { clusterManager.setOnClusterInfoWindowClickListener(null) }
        verify(exactly = 1) { clusterManager.setOnClusterInfoWindowLongClickListener(any()) }
    }

    @Test
    public fun testInstalledListenerCallsLatestCallback() {
        val listener = slot<OnClusterInfoWindowClickListener<TestItem>>()
        every { clusterManager.setOnClusterInfoWindowClickListener(capture(listener)) } just runs
        val calls = mutableListOf<String>()
        onClick = { calls += "first" }
        compose()

        onClick = { calls += "second" }
        compose()
        listener.captured.onClusterInfoWindowClick(cluster)

        assertEquals(listOf("second"), calls)
    }

    // Each later call to setContent recomposes the composition with the current callbacks.
    private fun compose() {
        composition.setContent {
            ClusterInfoWindowClickListeners(clusterManager, onClick, onLongClick)
        }
    }
}
