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

import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.Recomposer
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.common.truth.Truth.assertThat
import com.google.maps.android.robolectric.annotation.EnableMapsShadows
import com.google.maps.android.robolectric.ktx.createTestMapView
import com.google.maps.android.robolectric.ktx.getMapBlocking
import com.google.maps.android.robolectric.ktx.shadow
import com.google.maps.android.robolectric.shadows.ShadowGoogleMap
import com.google.maps.android.robolectric.truth.LatLngSubject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.EmptyCoroutineContext

// [START maps_compose_map_applier_test]
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@EnableMapsShadows
internal class MapApplierTest {

    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var shadowMap: ShadowGoogleMap
    private lateinit var applier: MapApplier

    private val sydney: LatLng = LatLng(-33.852, 151.211)
    private val perth: LatLng = LatLng(-31.952, 115.857)

    @Before
    internal fun setUp() {
        mapView = createTestMapView()
        googleMap = mapView.getMapBlocking()
        shadowMap = googleMap.shadow
        applier = MapApplier(googleMap, mapView, MapClickListeners(), isLiteMode = false)
    }

    private fun createMarkerNode(
        markerState: MarkerState,
        onMarkerClick: (Marker) -> Boolean = { false },
        position: LatLng = sydney,
    ): MarkerNode {
        val marker = googleMap.addMarker(MarkerOptions().position(position).title("Test Marker"))!!
        val dummyContext: CompositionContext = Recomposer(EmptyCoroutineContext)
        return MarkerNode(
            compositionContext = dummyContext,
            marker = marker,
            markerState = markerState,
            onMarkerClick = onMarkerClick,
            onInfoWindowClick = {},
            onInfoWindowClose = {},
            onInfoWindowLongClick = {},
            infoWindow = null,
            infoContent = null,
        )
    }

    @Test
    internal fun testInsertMarkerNode_attachesAndSyncsMarkerState() {
        val markerState = MarkerState(sydney)
        val node = createMarkerNode(markerState)

        applier.insertBottomUp(0, node)

        assertThat(markerState.marker).isSameInstanceAs(node.marker)
        assertThat(shadowMap.markers).contains(node.marker)
    }

    @Test
    internal fun testRemoveMarkerNode_clearsMarkerStateAndRemovesFromMap() {
        val markerState = MarkerState(sydney)
        val node = createMarkerNode(markerState)

        applier.insertBottomUp(0, node)
        assertThat(shadowMap.markers).hasSize(1)

        applier.remove(0, 1)

        assertThat(markerState.marker).isNull()
        assertThat(shadowMap.markers).isEmpty()
    }

    @Test
    internal fun testMarkerClick_dispatchesToNodeCallback() {
        val markerState = MarkerState(sydney)
        var clickCount = 0
        val node = createMarkerNode(
            markerState = markerState,
            onMarkerClick = {
                clickCount++
                true
            },
        )

        applier.insertBottomUp(0, node)

        // Simulate marker click via shadow
        shadowMap.clickMarker(node.marker)

        assertThat(clickCount).isEqualTo(1)
    }

    @Test
    internal fun testMarkerDrag_updatesMarkerStatePosition() {
        val markerState = MarkerState(sydney)
        val node = createMarkerNode(markerState)
        node.marker.isDraggable = true

        applier.insertBottomUp(0, node)

        // Drag marker to Perth via shadow
        shadowMap.dragMarkerStart(node.marker)
        assertThat(markerState.isDragging).isTrue()

        shadowMap.dragMarker(node.marker, perth)
        LatLngSubject.assertThat(markerState.position).isWithin(1.0).of(perth)

        shadowMap.dragMarkerEnd(node.marker, perth)
        assertThat(markerState.isDragging).isFalse()
        LatLngSubject.assertThat(markerState.position).isWithin(1.0).of(perth)
    }

    @Test
    internal fun testCircleNode_clickAndRemoval() {
        val circle = googleMap.addCircle(CircleOptions().center(sydney).radius(500.0).clickable(true))
        var circleClicked = false
        val circleNode = CircleNode(
            circle = circle,
            onCircleClick = { circleClicked = true },
        )

        applier.insertBottomUp(0, circleNode)
        assertThat(shadowMap.circles).contains(circle)

        // Simulate click via shadow
        shadowMap.clickCircle(circle)
        assertThat(circleClicked).isTrue()

        // Remove node
        applier.remove(0, 1)
        assertThat(shadowMap.circles).isEmpty()
    }

    @Test
    internal fun testOnClear_removesAllDecorations() {
        val markerState = MarkerState(sydney)
        val markerNode = createMarkerNode(markerState)
        val circle = googleMap.addCircle(CircleOptions().center(sydney).radius(500.0))
        val circleNode = CircleNode(circle, onCircleClick = {})

        applier.insertBottomUp(0, markerNode)
        applier.insertBottomUp(1, circleNode)

        assertThat(shadowMap.markers).hasSize(1)
        assertThat(shadowMap.circles).hasSize(1)

        applier.clear()

        assertThat(shadowMap.markers).isEmpty()
        assertThat(shadowMap.circles).isEmpty()
        assertThat(markerState.marker).isNull()
    }
}
// [END maps_compose_map_applier_test]
