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

import android.content.ComponentCallbacks2
import android.content.Context
import android.util.Log
import android.view.View
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.StreetViewPanoramaView
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.TileOverlay
import com.google.common.truth.Truth.assertThat
import com.google.maps.android.compose.streetview.componentCallbacks2
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
                position = CameraPosition.fromLatLngZoom(LatLng(1.0, 2.0), 10f),
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

    @Test
    public fun `MapTagData disposeSubcomposition cancels job and disposes composition`() {
        val mapView = mockk<MapView>(relaxed = true)
        val job = mockk<Job>(relaxed = true)
        val composition = mockk<Composition>(relaxed = true)
        val tagData = MapTagData(
            componentCallbacks = mockk<ComponentCallbacks2>(relaxed = true),
            lifecycleObserver = MapLifecycleEventObserver(mapView),
            detachLifecycleObserver = {},
            onAttachStateChangeListener = mockk<View.OnAttachStateChangeListener>(relaxed = true),
            isLiteMode = false,
        )
        tagData.subcompositionJob = job
        tagData.subcomposition = composition

        tagData.disposeSubcomposition()

        assertThat(tagData.subcompositionJob).isNull()
        assertThat(tagData.subcomposition).isNull()
        verifyOrder {
            job.cancel(null)
            composition.dispose()
        }
    }

    @Test
    public fun `MapLifecycleEventObserver transitions MapView through lifecycle states in order`() {
        val mapView = mockk<MapView>(relaxed = true)
        val observer = MapLifecycleEventObserver(mapView)
        val owner = object : LifecycleOwner {
            val registry = LifecycleRegistry.createUnsafe(this)
            override val lifecycle: Lifecycle get() = registry
        }

        observer.onStateChanged(owner, Lifecycle.Event.ON_CREATE)
        observer.onStateChanged(owner, Lifecycle.Event.ON_START)
        observer.onStateChanged(owner, Lifecycle.Event.ON_RESUME)

        // Detaching / ON_DESTROY from parent lifecycle moves down to CREATED (not onDestroy)
        observer.moveToBaseState()

        // Explicit release moves to DESTROYED (invoking mapView.onDestroy())
        observer.moveToDestroyedState()

        verifyOrder {
            mapView.onCreate(any())
            mapView.onStart()
            mapView.onResume()
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    @Test
    public fun `MapView onRelease disposes subcomposition BEFORE mapView onDestroy and cleans up view state`() {
        val context = mockk<Context>(relaxed = true)
        val mapView = mockk<MapView>(relaxed = true)
        every { mapView.context } returns context
        val componentCallbacks = mockk<ComponentCallbacks2>(relaxed = true)
        val attachListener = mockk<View.OnAttachStateChangeListener>(relaxed = true)
        val lifecycleObserver = MapLifecycleEventObserver(mapView)
        val owner = object : LifecycleOwner {
            val registry = LifecycleRegistry.createUnsafe(this)
            override val lifecycle: Lifecycle get() = registry
        }
        lifecycleObserver.onStateChanged(owner, Lifecycle.Event.ON_CREATE)
        lifecycleObserver.onStateChanged(owner, Lifecycle.Event.ON_START)
        lifecycleObserver.onStateChanged(owner, Lifecycle.Event.ON_RESUME)

        var attachCleanupCalled = false
        val tagData = MapTagData(
            componentCallbacks = componentCallbacks,
            lifecycleObserver = lifecycleObserver,
            detachLifecycleObserver = { attachCleanupCalled = true },
            onAttachStateChangeListener = attachListener,
            isLiteMode = false,
        )
        val job = mockk<Job>(relaxed = true)
        val composition = mockk<Composition>(relaxed = true)
        tagData.subcompositionJob = job
        tagData.subcomposition = composition
        every { mapView.tag } returns tagData

        mapView.onRelease()

        assertThat(attachCleanupCalled).isTrue()
        verifyOrder {
            // 1. Subcomposition is cancelled and disposed synchronously first
            job.cancel(null)
            composition.dispose()
            // 2. ComponentCallbacks and AttachStateChangeListener are unregistered
            context.unregisterComponentCallbacks(componentCallbacks)
            mapView.removeOnAttachStateChangeListener(attachListener)
            // 3. MapView lifecycle transitions down to DESTROYED (mapView.onDestroy())
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
            // 4. Child views and tag are cleared
            mapView.removeAllViews()
            mapView.tag = null
        }
    }

    @Test
    public fun `MapView onReset disposes subcomposition without destroying MapView`() {
        val mapView = mockk<MapView>(relaxed = true)
        val lifecycleObserver = MapLifecycleEventObserver(mapView)
        val owner = object : LifecycleOwner {
            val registry = LifecycleRegistry.createUnsafe(this)
            override val lifecycle: Lifecycle get() = registry
        }
        lifecycleObserver.onStateChanged(owner, Lifecycle.Event.ON_CREATE)

        val tagData = MapTagData(
            componentCallbacks = mockk<ComponentCallbacks2>(relaxed = true),
            lifecycleObserver = lifecycleObserver,
            detachLifecycleObserver = {},
            onAttachStateChangeListener = mockk<View.OnAttachStateChangeListener>(relaxed = true),
            isLiteMode = false,
        )
        val job = mockk<Job>(relaxed = true)
        val composition = mockk<Composition>(relaxed = true)
        tagData.subcompositionJob = job
        tagData.subcomposition = composition
        every { mapView.tag } returns tagData

        mapView.onReset()

        // Subcomposition is disposed and reset to null so update{} will re-launch on reuse
        assertThat(tagData.subcompositionJob).isNull()
        assertThat(tagData.subcomposition).isNull()
        verifyOrder {
            job.cancel(null)
            composition.dispose()
        }
        // MapView itself is NOT destroyed on reset
        verify(exactly = 0) { mapView.onDestroy() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    public fun `launchSubcomposition aborts safely when MapView is released before awaitMap completes`(): Unit =
        runTest {
            val testDispatcher = UnconfinedTestDispatcher(testScheduler)
            Dispatchers.setMain(testDispatcher)
            try {
                val context = mockk<Context>(relaxed = true)
                val mapView = mockk<MapView>(relaxed = true)
                val map = mockk<GoogleMap>(relaxed = true)
                val callbackSlot = slot<OnMapReadyCallback>()
                every { mapView.context } returns context
                every { mapView.getMapAsync(capture(callbackSlot)) } returns Unit

                val tagData = MapTagData(
                    componentCallbacks = mockk<ComponentCallbacks2>(relaxed = true),
                    lifecycleObserver = MapLifecycleEventObserver(mapView),
                    detachLifecycleObserver = {},
                    onAttachStateChangeListener = mockk<View.OnAttachStateChangeListener>(relaxed = true),
                    isLiteMode = false,
                )
                var currentTag: Any? = tagData
                every { mapView.tag } answers { currentTag }
                every { mapView.tag = any() } answers { currentTag = firstArg() }

                val mapUpdaterState = MapUpdaterState(
                    mergeDescendants = false,
                    contentDescription = null,
                    cameraPositionState = CameraPositionState(),
                    contentPadding = PaddingValues(),
                    locationSource = null,
                    mapProperties = DefaultMapProperties,
                    mapUiSettings = DefaultMapUiSettings,
                    mapColorScheme = null,
                )

                // 1. Launch subcomposition (suspends at mapView.awaitMap())
                tagData.subcompositionJob = launchSubcomposition(
                    mapUpdaterState = mapUpdaterState,
                    parentComposition = mockk<CompositionContext>(relaxed = true),
                    mapView = mapView,
                    tagData = tagData,
                    mapClickListeners = MapClickListeners(),
                    content = {},
                )
                assertThat(callbackSlot.isCaptured).isTrue()

                // 2. Simulate rapid hide: MapView is released BEFORE getMapAsync callback fires
                mapView.onRelease()
                assertThat(currentTag).isNull()
                assertThat(tagData.subcompositionJob).isNull()

                // 3. Even if the Maps SDK still invokes the captured OnMapReadyCallback afterwards,
                // it must be a safe no-op (no NPE on mapView.tag and no listeners attached to map)
                callbackSlot.captured.onMapReady(map)
                assertThat(tagData.subcomposition).isNull()
                verify(exactly = 0) { map.setInfoWindowAdapter(any()) }
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    public fun `MapAttachStateChangeListener logs error and throws when ViewTreeLifecycleOwner is null`() {
        mockkStatic(Log::class)
        mockkStatic("androidx.lifecycle.ViewTreeLifecycleOwner")
        try {
            every { Log.e(any(), any()) } returns 0
            val mapView = mockk<MapView>(relaxed = true)
            every { mapView.findViewTreeLifecycleOwner() } returns null

            val listener = MapAttachStateChangeListener(MapLifecycleEventObserver(mapView))

            var thrown: IllegalStateException? = null
            try {
                listener.onViewAttachedToWindow(mapView)
            } catch (e: IllegalStateException) {
                thrown = e
            }

            assertThat(thrown).isNotNull()
            assertThat(thrown!!.message).contains("ViewTreeLifecycleOwner not found for MapView")
            verify {
                Log.e(
                    "GoogleMap",
                    match { it.contains("ViewTreeLifecycleOwner not found for MapView") },
                )
            }
        } finally {
            unmockkStatic("androidx.lifecycle.ViewTreeLifecycleOwner")
            unmockkStatic(Log::class)
        }
    }

    @Suppress("DEPRECATION")
    @Test
    public fun `MapView componentCallbacks2 only calls onLowMemory for UI_HIDDEN and higher`() {
        val mapView = mockk<MapView>(relaxed = true)
        val callbacks = mapView.componentCallbacks2()

        callbacks.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE)
        callbacks.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)
        callbacks.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL)
        verify(exactly = 0) { mapView.onLowMemory() }

        callbacks.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
        verify(exactly = 1) { mapView.onLowMemory() }

        callbacks.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
        verify(exactly = 2) { mapView.onLowMemory() }
    }

    @Suppress("DEPRECATION")
    @Test
    public fun `StreetViewPanoramaView componentCallbacks2 only calls onLowMemory for UI_HIDDEN and higher`() {
        val streetView = mockk<StreetViewPanoramaView>(relaxed = true)
        val callbacks = streetView.componentCallbacks2()

        callbacks.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE)
        callbacks.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)
        callbacks.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL)
        verify(exactly = 0) { streetView.onLowMemory() }

        callbacks.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
        verify(exactly = 1) { streetView.onLowMemory() }

        callbacks.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
        verify(exactly = 2) { streetView.onLowMemory() }
    }

    @Test
    public fun `MapLifecycleEventObserver forwards all lifecycle events to custom AbstractMapViewDelegate`() {
        val customDelegate = mockk<AbstractMapViewDelegate<View>>(relaxed = true)
        val observer = MapLifecycleEventObserver(customDelegate)
        val owner = object : LifecycleOwner {
            val registry = LifecycleRegistry.createUnsafe(this)
            override val lifecycle: Lifecycle get() = registry
        }

        observer.onStateChanged(owner, Lifecycle.Event.ON_CREATE)
        observer.onStateChanged(owner, Lifecycle.Event.ON_START)
        observer.onStateChanged(owner, Lifecycle.Event.ON_RESUME)
        observer.moveToBaseState()
        observer.moveToDestroyedState()

        verifyOrder {
            customDelegate.onCreate(any())
            customDelegate.onStart()
            customDelegate.onResume()
            customDelegate.onPause()
            customDelegate.onStop()
            customDelegate.onDestroy()
        }
    }

    @Suppress("DEPRECATION")
    @Test
    public fun `AbstractMapViewDelegate componentCallbacks2 forwards onLowMemory and UI_HIDDEN trimMemory`() {
        val customDelegate = mockk<AbstractMapViewDelegate<View>>(relaxed = true)
        val callbacks = customDelegate.componentCallbacks2()

        callbacks.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)
        verify(exactly = 0) { customDelegate.onLowMemory() }

        callbacks.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
        verify(exactly = 1) { customDelegate.onLowMemory() }

        callbacks.onLowMemory()
        verify(exactly = 2) { customDelegate.onLowMemory() }
    }
}
