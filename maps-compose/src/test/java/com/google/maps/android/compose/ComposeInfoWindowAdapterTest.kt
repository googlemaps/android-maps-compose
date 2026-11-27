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

import android.os.Trace
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.Recomposer
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkStatic
import org.junit.Test
import kotlin.coroutines.EmptyCoroutineContext

public class ComposeInfoWindowAdapterTest {

    private class Render(val parentContext: CompositionContext, val content: @Composable () -> Unit)

    private val renders = mutableListOf<Render>()
    private val renderedView = mockk<View>()

    private fun adapter(
        markerNodes: List<MarkerNode> = emptyList(),
        handlers: List<InfoWindowHandlerNode> = emptyList(),
    ) = ComposeInfoWindowAdapter(
        mapView = mockk<MapView>(relaxed = true),
        markerNodeFinder = { marker -> markerNodes.firstOrNull { it.marker == marker } },
        infoWindowHandlers = { handlers },
        renderToView = { parentContext, content ->
            renders += Render(parentContext, content)
            renderedView
        },
    )

    private fun markerNode(
        marker: Marker,
        infoWindow: (@Composable (Marker) -> Unit)? = null,
        infoContent: (@Composable (Marker) -> Unit)? = null,
    ) = MarkerNode(
        compositionContext = mockk(relaxed = true),
        marker = marker,
        markerState = MarkerState(position = LatLng(1.0, 2.0)),
        onMarkerClick = { false },
        onInfoWindowClick = {},
        onInfoWindowClose = {},
        onInfoWindowLongClick = {},
        infoWindow = infoWindow,
        infoContent = infoContent,
    )

    private fun handler(
        infoWindow: ((Marker) -> (@Composable () -> Unit)?)? = null,
        infoContent: ((Marker) -> (@Composable () -> Unit)?)? = null,
    ) = InfoWindowHandlerNode(
        compositionContext = mockk(relaxed = true),
        infoWindow = infoWindow,
        infoContent = infoContent,
    )

    @Test
    public fun `a marker owned by a MarkerNode renders in that node's composition without asking handlers`() {
        val marker = mockk<Marker>()
        val node = markerNode(marker, infoWindow = {})
        var handlerAsked = false
        val handlerContent: @Composable () -> Unit = {}
        val handler = handler(infoWindow = { handlerContent.also { handlerAsked = true } })

        val view = adapter(markerNodes = listOf(node), handlers = listOf(handler)).getInfoWindow(marker)

        assertThat(view).isSameInstanceAs(renderedView)
        assertThat(renders.single().parentContext).isSameInstanceAs(node.compositionContext)
        assertThat(handlerAsked).isFalse()
    }

    @Test
    public fun `a marker owned by a MarkerNode without info window content gets the default window, not a handler's`() {
        val marker = mockk<Marker>()
        val handler = handler(infoWindow = { {} }, infoContent = { {} })
        val adapter = adapter(markerNodes = listOf(markerNode(marker)), handlers = listOf(handler))

        assertThat(adapter.getInfoWindow(marker)).isNull()
        assertThat(adapter.getInfoContents(marker)).isNull()
        assertThat(renders).isEmpty()
    }

    @Test
    public fun `a marker owned by a MarkerNode gets that node's info content only from getInfoContents`() {
        val marker = mockk<Marker>()
        val node = markerNode(marker, infoContent = {})
        val adapter = adapter(markerNodes = listOf(node))

        assertThat(adapter.getInfoWindow(marker)).isNull()
        assertThat(adapter.getInfoContents(marker)).isSameInstanceAs(renderedView)
        assertThat(renders.single().parentContext).isSameInstanceAs(node.compositionContext)
    }

    @Test
    public fun `an unowned marker renders the first content a handler supplies, in that handler's composition`() {
        val marker = mockk<Marker>()
        val content: @Composable () -> Unit = {}
        val withoutLookups = handler()
        val declining = handler(infoWindow = { null })
        val supplying = handler(infoWindow = { asked -> content.takeIf { asked == marker } })
        val later = handler(infoWindow = { {} })

        val view = adapter(handlers = listOf(withoutLookups, declining, supplying, later)).getInfoWindow(marker)

        assertThat(view).isSameInstanceAs(renderedView)
        val render = renders.single()
        assertThat(render.parentContext).isSameInstanceAs(supplying.compositionContext)
        assertThat(render.content).isSameInstanceAs(content)
    }

    @Test
    public fun `an unowned marker gets a handler's info content only from getInfoContents`() {
        val marker = mockk<Marker>()
        val content: @Composable () -> Unit = {}
        val handler = handler(infoContent = { content })
        val adapter = adapter(handlers = listOf(handler))

        assertThat(adapter.getInfoWindow(marker)).isNull()
        assertThat(adapter.getInfoContents(marker)).isSameInstanceAs(renderedView)
        assertThat(renders.single().content).isSameInstanceAs(content)
    }

    @Test
    public fun `an unowned marker that no handler supplies gets the default window`() {
        val marker = mockk<Marker>()
        val adapter = adapter(handlers = listOf(handler(infoWindow = { null }, infoContent = { null })))

        assertThat(adapter.getInfoWindow(marker)).isNull()
        assertThat(adapter.getInfoContents(marker)).isNull()
        assertThat(renders).isEmpty()
    }

    @Test
    public fun `MapApplier's info window adapter asks its InfoWindowHandler nodes`() {
        val map = mockk<GoogleMap>(relaxed = true)
        val installedAdapter = slot<GoogleMap.InfoWindowAdapter>()
        every { map.setInfoWindowAdapter(capture(installedAdapter)) } just runs
        val applier = MapApplier(
            map = map,
            mapView = mockk(relaxed = true),
            mapClickListeners = MapClickListeners(),
            isLiteMode = false,
        )
        val askedMarkers = mutableListOf<Marker>()
        applier.insertBottomUp(0, handler(infoWindow = { marker -> null.also { askedMarkers += marker } }))
        val marker = mockk<Marker>()

        assertThat(installedAdapter.captured.getInfoWindow(marker)).isNull()
        assertThat(askedMarkers).containsExactly(marker)
    }

    @Test
    public fun `InfoWindowHandler gives the info window adapter its latest lookups`() {
        val map = mockk<GoogleMap>(relaxed = true)
        val installedAdapter = slot<GoogleMap.InfoWindowAdapter>()
        every { map.setInfoWindowAdapter(capture(installedAdapter)) } just runs
        val applier = MapApplier(
            map = map,
            mapView = mockk(relaxed = true),
            mapClickListeners = MapClickListeners(),
            isLiteMode = false,
        )
        val composition = Composition(applier, Recomposer(EmptyCoroutineContext))
        val askedLookups = mutableListOf<String>()
        var lookupName = "first"
        // Each later call to setContent recomposes the composition with the current lookup name.
        val compose = {
            composition.setContent {
                // The read happens during composition, so each composition gives InfoWindowHandler its own lookup.
                val name = lookupName
                InfoWindowHandler(infoWindow = { null.also { askedLookups += name } })
            }
        }
        mockkStatic(Trace::class)
        try {
            every { Trace.beginSection(any()) } just runs
            every { Trace.endSection() } just runs
            compose()
            lookupName = "second"
            compose()

            installedAdapter.captured.getInfoWindow(mockk<Marker>())
        } finally {
            composition.dispose()
            unmockkStatic(Trace::class)
        }

        assertThat(askedLookups).containsExactly("second")
    }
}
