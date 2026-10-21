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
import androidx.compose.ui.geometry.Offset
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.ui.RotationLayout
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.concurrent.Executor

@OptIn(ExperimentalCoroutinesApi::class)
public class ClusterRendererTest {

    private data class TestItem(
        override val position: LatLng,
        override val title: String? = "Title",
        override val snippet: String? = "Snippet",
        override val zIndex: Float? = 0f,
    ) : ClusterItem

    @Test
    public fun testClusterManagerAndReportingRendererSupportCustomContextsAndExecutor() {
        mockkStatic(LayoutInflater::class)
        mockkConstructor(ShapeDrawable::class)
        try {
            every { anyConstructed<ShapeDrawable>().paint } returns mockk(relaxed = true)
            val inflater = mockk<LayoutInflater>(relaxed = true)
            val rotationLayout = mockk<RotationLayout>(relaxed = true)
            every { rotationLayout.findViewById<View>(any()) } returns mockk<TextView>(relaxed = true)
            val container = mockk<ViewGroup>(relaxed = true)
            every { container.getChildAt(0) } returns rotationLayout
            every { inflater.inflate(any<Int>(), any()) } returns container
            every { LayoutInflater.from(any()) } returns inflater
            val context = mockk<Context>(relaxed = true)
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
        } finally {
            unmockkConstructor(ShapeDrawable::class)
            unmockkStatic(LayoutInflater::class)
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
}
