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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.TileOverlay
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.Test

public class MapApplierCleanupTest {

    @Test
    public fun `MapApplier onClear detaches all GoogleMap listeners and InfoWindowAdapter`() {
        val map = mockk<GoogleMap>(relaxed = true)
        val mapView = mockk<MapView>(relaxed = true)
        val mapClickListeners = MapClickListeners()

        val applier = MapApplier(
            map = map,
            mapView = mapView,
            mapClickListeners = mapClickListeners,
            isLiteMode = false,
        )

        var nodeCleared = false
        val node = object : MapNode {
            override fun onCleared() {
                nodeCleared = true
            }
        }
        applier.insertBottomUp(0, node)

        applier.clear()

        assertThat(nodeCleared).isTrue()
        verify { map.clear() }
        verify { map.setOnCircleClickListener(null) }
        verify { map.setOnGroundOverlayClickListener(null) }
        verify { map.setOnPolygonClickListener(null) }
        verify { map.setOnPolylineClickListener(null) }
        verify { map.setOnMarkerClickListener(null) }
        verify { map.setOnInfoWindowClickListener(null) }
        verify { map.setOnInfoWindowCloseListener(null) }
        verify { map.setOnInfoWindowLongClickListener(null) }
        verify { map.setOnMarkerDragListener(null) }
        verify { map.setInfoWindowAdapter(null) }
    }

    @Test
    public fun `MapPropertiesNode onCleared and onRemoved detach camera listeners and locationSource`() {
        mockkStatic(CameraUpdateFactory::class)
        try {
            every { CameraUpdateFactory.newCameraPosition(any()) } returns mockk(relaxed = true)
            val map = mockk<GoogleMap>(relaxed = true)
            val cameraPositionState = CameraPositionState(
                position = CameraPosition.fromLatLngZoom(LatLng(1.0, 2.0), 10f)
            )
            val node = MapPropertiesNode(
                map = map,
                cameraPositionState = cameraPositionState,
                contentDescription = "Test Map",
                density = Density(1f),
                layoutDirection = LayoutDirection.Ltr,
                contentPadding = PaddingValues(),
            )

            node.onAttached()
            node.onCleared()

            verify { map.setOnCameraIdleListener(null) }
            verify { map.setOnCameraMoveCanceledListener(null) }
            verify { map.setOnCameraMoveStartedListener(null) }
            verify { map.setOnCameraMoveListener(null) }
            verify { map.setLocationSource(null) }
        } finally {
            unmockkStatic(CameraUpdateFactory::class)
        }
    }

    @Test
    public fun `TileOverlayNode onCleared and onRemoved clear tileOverlayState reference`() {
        val tileOverlay = mockk<TileOverlay>(relaxed = true)
        val tileOverlayState = TileOverlayState()
        val node = TileOverlayNode(
            tileOverlay = tileOverlay,
            tileOverlayState = tileOverlayState,
            onTileOverlayClick = {},
        )

        node.onAttached()
        assertThat(tileOverlayState.tileOverlay).isSameInstanceAs(tileOverlay)

        node.onCleared()
        assertThat(tileOverlayState.tileOverlay).isNull()
        verify { tileOverlay.remove() }
    }

    @Test
    public fun `MarkerNode onCleared and onRemoved clear markerState reference`() {
        val marker = mockk<Marker>(relaxed = true)
        val markerState = MarkerState(position = LatLng(1.0, 2.0))
        val node = MarkerNode(
            compositionContext = mockk<CompositionContext>(relaxed = true),
            marker = marker,
            markerState = markerState,
            onMarkerClick = { false },
            onInfoWindowClick = {},
            onInfoWindowClose = {},
            onInfoWindowLongClick = {},
            infoWindow = null,
            infoContent = null,
        )

        node.onAttached()
        assertThat(markerState.marker).isSameInstanceAs(marker)

        node.onCleared()
        assertThat(markerState.marker).isNull()
        verify { marker.remove() }
    }
}
