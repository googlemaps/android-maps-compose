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

import android.content.Context
import android.graphics.drawable.ShapeDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.compose.ComposeUiViewRenderer
import com.google.maps.android.ui.RotationLayout
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.Executor

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
public class ClusterRendererTest {

    private data class TestItem(
        override val position: LatLng,
        override val title: String? = "Title",
        override val snippet: String? = "Snippet",
        override val zIndex: Float? = 0f,
    ) : ClusterItem

    private inline fun <R> withRendererDependencies(block: (Context, GoogleMap, ClusterManager<TestItem>) -> R): R {
        mockkStatic(LayoutInflater::class)
        mockkStatic(ContextCompat::class)
        try {
            every { ContextCompat.getDrawable(any(), any()) } returns ShapeDrawable()
            val inflater = mockk<LayoutInflater>(relaxed = true)
            val rotationLayout = mockk<RotationLayout>(relaxed = true)
            every { rotationLayout.findViewById<View>(any()) } returns mockk<TextView>(relaxed = true)
            val container = mockk<ViewGroup>(relaxed = true)
            every { container.getChildAt(0) } returns rotationLayout
            every { inflater.inflate(any<Int>(), any()) } returns container
            every { LayoutInflater.from(any()) } returns inflater

            val context: Context = RuntimeEnvironment.getApplication()
            val map = mockk<GoogleMap>(relaxed = true)
            val mainDispatcher = UnconfinedTestDispatcher()
            val backgroundDispatcher = UnconfinedTestDispatcher()
            val clusterManager = ClusterManager<TestItem>(
                context,
                map,
                mockk(relaxed = true),
                mainDispatcher,
                backgroundDispatcher,
            )
            return block(context, map, clusterManager)
        } finally {
            unmockkStatic(ContextCompat::class)
            unmockkStatic(LayoutInflater::class)
        }
    }

    @Test
    public fun testClusterManagerAndReportingRendererSupportCustomContextsAndExecutor() {
        withRendererDependencies { context, map, clusterManager ->
            assertNotNull(clusterManager)

            val customExecutor = Executor { runnable -> runnable.run() }
            val renderer = ReportingDefaultClusterRenderer(
                context = context,
                map = map,
                clusterManager = clusterManager,
                executor = customExecutor,
            )
            assertNotNull(renderer)
            assertEquals(emptySet<TestItem>(), renderer.unclusteredItems.value)
        }
    }

    @Test
    public fun testOnClusterItemUpdatedPreservesCustomZIndexAndAvoidsRedrawingCachedView() {
        withRendererDependencies { context, map, clusterManager ->
            val scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher())
            mockkStatic(BitmapDescriptorFactory::class)
            try {
                val initialIcon = mockk<BitmapDescriptor>()
                every { BitmapDescriptorFactory.fromBitmap(any()) } returns initialIcon

                val viewRenderer = mockk<ComposeUiViewRenderer>(relaxed = true)
                val renderer = ComposeUiClusterRenderer(
                    context = context,
                    scope = scope,
                    map = map,
                    clusterManager = clusterManager,
                    viewRendererState = mutableStateOf(viewRenderer),
                    clusterContentState = mutableStateOf(null),
                    clusterItemContentState = mutableStateOf({}),
                    clusterContentAnchorState = mutableStateOf(Offset(0.5f, 1.0f)),
                    clusterItemContentAnchorState = mutableStateOf(Offset(0.25f, 0.75f)),
                    clusterContentZIndexState = mutableStateOf(0.0f),
                    clusterItemContentZIndexState = mutableStateOf(9.0f),
                    clusterContentRotationState = mutableStateOf(0.0f),
                    clusterItemContentRotationState = mutableStateOf(45.0f),
                    executor = Executor { runnable -> runnable.run() },
                )

                val item = TestItem(
                    position = LatLng(37.4220, -122.0841),
                    title = "Updated Title",
                    snippet = "Updated Snippet",
                    zIndex = 0f,
                )

                // Initial render creates and caches the Compose view and renders its bitmap once.
                val markerOptions = MarkerOptions()
                renderer.onBeforeClusterItemRendered(item, markerOptions)
                assertEquals(9.0f, markerOptions.zIndex, 0.0f)
                verify(exactly = 1) { BitmapDescriptorFactory.fromBitmap(any()) }

                val marker = mockk<Marker>(relaxed = true)
                every { marker.position } returns item.position
                every { marker.title } returns "Old Title"
                every { marker.snippet } returns "Old Snippet"
                every { marker.zIndex } returns 9.0f
                every { marker.isInfoWindowShown } returns false
                every { marker.setAnchor(any(), any()) } just runs
                every { marker.zIndex = any() } just runs
                every { marker.rotation = any() } just runs

                renderer.onClusterItemUpdated(item, marker)

                verifyOrder {
                    // DefaultClusterRenderer.onClusterItemUpdated overwrites zIndex with item.zIndex (0f)
                    marker.zIndex = 0f
                    // ComposeUiClusterRenderer.onClusterItemUpdated restores the custom Compose zIndex (9.0f)
                    marker.zIndex = 9.0f
                }
                verify { marker.setAnchor(0.25f, 0.75f) }
                verify { marker.rotation = 45.0f }
                verify(exactly = 0) { marker.setIcon(any()) }
                // No additional bitmap rendering occurred during onClusterItemUpdated.
                verify(exactly = 1) { BitmapDescriptorFactory.fromBitmap(any()) }
            } finally {
                unmockkStatic(BitmapDescriptorFactory::class)
                scope.cancel()
            }
        }
    }

    @Test
    public fun testOnClusterUpdatedPreservesCustomMarkerProperties() {
        withRendererDependencies { context, map, clusterManager ->
            val scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher())
            mockkStatic(BitmapDescriptorFactory::class)
            try {
                val clusterIcon = mockk<BitmapDescriptor>()
                every { BitmapDescriptorFactory.fromBitmap(any()) } returns clusterIcon

                val viewRenderer = mockk<ComposeUiViewRenderer>(relaxed = true)
                val renderer = ComposeUiClusterRenderer(
                    context = context,
                    scope = scope,
                    map = map,
                    clusterManager = clusterManager,
                    viewRendererState = mutableStateOf(viewRenderer),
                    clusterContentState = mutableStateOf({}),
                    clusterItemContentState = mutableStateOf(null),
                    clusterContentAnchorState = mutableStateOf(Offset(0.3f, 0.7f)),
                    clusterItemContentAnchorState = mutableStateOf(Offset(0.5f, 1.0f)),
                    clusterContentZIndexState = mutableStateOf(15.0f),
                    clusterItemContentZIndexState = mutableStateOf(0.0f),
                    clusterContentRotationState = mutableStateOf(60.0f),
                    clusterItemContentRotationState = mutableStateOf(0.0f),
                    executor = Executor { runnable -> runnable.run() },
                )

                val cluster = mockk<Cluster<TestItem>>(relaxed = true)
                every { cluster.size } returns 10
                every { cluster.position } returns LatLng(37.4220, -122.0841)
                val marker = mockk<Marker>(relaxed = true)
                every { marker.setIcon(any()) } just runs
                every { marker.setAnchor(any(), any()) } just runs
                every { marker.zIndex = any() } just runs
                every { marker.rotation = any() } just runs

                renderer.onClusterUpdated(cluster, marker)

                verify { marker.setAnchor(0.3f, 0.7f) }
                verify { marker.zIndex = 15.0f }
                verify { marker.rotation = 60.0f }
            } finally {
                unmockkStatic(BitmapDescriptorFactory::class)
                scope.cancel()
            }
        }
    }

    @Test
    public fun testApplyClusterMarkerPropertiesUsesDefaultsWhenPropertiesNull() {
        val marker = mockk<Marker>(relaxed = true)
        every { marker.setAnchor(any(), any()) } just runs
        every { marker.zIndex = any() } just runs
        every { marker.rotation = any() } just runs

        applyClusterMarkerProperties(
            marker = marker,
            properties = null,
            defaultAnchor = Offset(0.25f, 0.75f),
            defaultZIndex = 5.0f,
            defaultRotation = 45.0f,
        )

        verify { marker.setAnchor(0.25f, 0.75f) }
        verify { marker.zIndex = 5.0f }
        verify { marker.rotation = 45.0f }
    }

    @Test
    public fun testApplyClusterMarkerPropertiesPrefersModifierOverrides() {
        val marker = mockk<Marker>(relaxed = true)
        every { marker.setAnchor(any(), any()) } just runs
        every { marker.zIndex = any() } just runs
        every { marker.rotation = any() } just runs

        val properties = ClusteringMarkerProperties().apply {
            anchor = Offset(0.1f, 0.2f)
            zIndex = 9.0f
            rotation = 180.0f
        }

        applyClusterMarkerProperties(
            marker = marker,
            properties = properties,
            defaultAnchor = Offset(0.5f, 1.0f),
            defaultZIndex = 0.0f,
            defaultRotation = 0.0f,
        )

        verify { marker.setAnchor(0.1f, 0.2f) }
        verify { marker.zIndex = 9.0f }
        verify { marker.rotation = 180.0f }
    }

    @Test
    public fun testApplyClusterItemMarkerPropertiesUpdatesIconAndMarkerProperties() {
        val marker = mockk<Marker>(relaxed = true)
        val icon = mockk<BitmapDescriptor>()
        every { marker.setIcon(any()) } just runs
        every { marker.setAnchor(any(), any()) } just runs
        every { marker.zIndex = any() } just runs
        every { marker.rotation = any() } just runs

        val properties = ClusteringMarkerProperties()

        applyClusterItemMarkerProperties(
            marker = marker,
            icon = icon,
            properties = properties,
            defaultAnchor = Offset(0.1f, 0.9f),
            defaultZIndex = 3.0f,
            defaultRotation = 90.0f,
        )

        verify { marker.setIcon(icon) }
        verify { marker.setAnchor(0.1f, 0.9f) }
        verify { marker.zIndex = 3.0f }
        verify { marker.rotation = 90.0f }
    }

    @Test
    public fun testApplyClusterItemMarkerPropertiesSkipsIconUpdateWhenIconNull() {
        val marker = mockk<Marker>(relaxed = true)
        every { marker.setAnchor(any(), any()) } just runs
        every { marker.zIndex = any() } just runs
        every { marker.rotation = any() } just runs

        val properties = ClusteringMarkerProperties().apply {
            zIndex = 7.0f
            rotation = 30.0f
            anchor = Offset(0.2f, 0.8f)
        }

        applyClusterItemMarkerProperties(
            marker = marker,
            icon = null,
            properties = properties,
            defaultAnchor = Offset(0.5f, 1.0f),
            defaultZIndex = 0.0f,
            defaultRotation = 0.0f,
        )

        verify(exactly = 0) { marker.setIcon(any()) }
        verify { marker.setAnchor(0.2f, 0.8f) }
        verify { marker.zIndex = 7.0f }
        verify { marker.rotation = 30.0f }
    }
}
