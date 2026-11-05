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

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.maps.MapView
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Test

public class MapLifecycleEventObserverTest {
    @Test
    public fun `moveToDestroyedState still walks the MapView down to onDestroy`() {
        val mapView = mockk<MapView>(relaxed = true)
        val observer = MapLifecycleEventObserver(mapView)
        val owner = mockk<LifecycleOwner>(relaxed = true)

        observer.onStateChanged(owner, Lifecycle.Event.ON_RESUME)
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
    public fun `lifecycle event after DESTROYED does not throw or move the MapView`() {
        val mapView = mockk<MapView>(relaxed = true)
        val observer = MapLifecycleEventObserver(mapView)
        val owner = mockk<LifecycleOwner>(relaxed = true)

        observer.onStateChanged(owner, Lifecycle.Event.ON_RESUME)
        observer.moveToDestroyedState()

        observer.onStateChanged(owner, Lifecycle.Event.ON_STOP)
        observer.onStateChanged(owner, Lifecycle.Event.ON_RESUME)
        observer.onStateChanged(owner, Lifecycle.Event.ON_DESTROY)
        observer.moveToDestroyedState()

        verify(exactly = 1) { mapView.onCreate(any()) }
        verify(exactly = 1) { mapView.onStart() }
        verify(exactly = 1) { mapView.onResume() }
        verify(exactly = 1) { mapView.onPause() }
        verify(exactly = 1) { mapView.onStop() }
        verify(exactly = 1) { mapView.onDestroy() }
    }
}
