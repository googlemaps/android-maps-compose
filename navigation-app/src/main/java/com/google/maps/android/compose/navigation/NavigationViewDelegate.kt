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

package com.google.maps.android.compose.navigation

import android.content.ComponentCallbacks2
import android.os.Bundle
import com.google.android.gms.maps.GoogleMap
import com.google.android.libraries.navigation.NavigationView
import com.google.android.libraries.navigation.OnNavigationUiChangedListener
import com.google.maps.android.compose.MapViewDelegate
import com.google.maps.android.compose.MapsComposeExperimentalApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@OptIn(MapsComposeExperimentalApi::class)
class NavigationViewDelegate(
    override val mapView: NavigationView,
    private val onNavigationUiChanged: ((Boolean) -> Unit)? = null,
) : MapViewDelegate<NavigationView> {
    private var navigationUiListener: OnNavigationUiChangedListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        mapView.onCreate(savedInstanceState)
        navigationUiListener?.let { mapView.removeOnNavigationUiChangedListener(it) }
        navigationUiListener = onNavigationUiChanged?.let { callback ->
            callback(mapView.isNavigationUiEnabled)
            OnNavigationUiChangedListener { enabled ->
                callback(enabled)
            }.also { listener ->
                mapView.addOnNavigationUiChangedListener(listener)
            }
        }
    }

    override fun onStart(): Unit = mapView.onStart()
    override fun onResume(): Unit = mapView.onResume()
    override fun onPause(): Unit = mapView.onPause()
    override fun onStop(): Unit = mapView.onStop()

    override fun onLowMemory() {
        mapView.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
    }

    override fun onTrimMemory(level: Int) {
        mapView.onTrimMemory(level)
    }

    override fun onDestroy() {
        navigationUiListener?.let { listener ->
            mapView.removeOnNavigationUiChangedListener(listener)
        }
        navigationUiListener = null
        mapView.onDestroy()
    }

    override suspend fun awaitMap(): GoogleMap = mapView.awaitMap()
}

suspend inline fun NavigationView.awaitMap(): GoogleMap = suspendCancellableCoroutine { continuation ->
    getMapAsync { map ->
        if (continuation.isActive) {
            continuation.resume(map)
        }
    }
}
