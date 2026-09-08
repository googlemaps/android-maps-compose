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

package com.google.maps.android.compose.streetview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.currentComposer
import com.google.android.gms.maps.StreetViewPanorama
import com.google.maps.android.compose.MapNode

internal class StreetViewPanoramaPropertiesNode(
    val cameraPositionState: StreetViewCameraPositionState,
    val panorama: StreetViewPanorama,
    var eventListeners: StreetViewPanoramaEventListeners,
) : MapNode {
    init {
        cameraPositionState.panorama = panorama
    }

    override fun onAttached() {
        super.onAttached()
        panorama.setOnStreetViewPanoramaClickListener {
            eventListeners.onClick(it)
        }
        panorama.setOnStreetViewPanoramaLongClickListener {
            eventListeners.onLongClick(it)
        }
        // Both Java listener interfaces lack @NonNull, so GMS can deliver null. Guard the
        // assignments to prevent the Kotlin compiler's implicit null-check from crashing.
        panorama.setOnStreetViewPanoramaCameraChangeListener {
            it?.let { cameraPositionState.rawPanoramaCamera = it }
        }
        panorama.setOnStreetViewPanoramaChangeListener {
            it?.let { cameraPositionState.rawLocation = it }
        }
    }

    override fun onRemoved() {
        cameraPositionState.panorama = null
    }

    override fun onCleared() {
        cameraPositionState.panorama = null
    }
}

/**
 * Used to keep the street view panorama properties up-to-date.
 */
@Suppress("NOTHING_TO_INLINE")
@Composable
internal inline fun StreetViewUpdater(
    cameraPositionState: StreetViewCameraPositionState,
    isPanningGesturesEnabled: Boolean,
    isStreetNamesEnabled: Boolean,
    isUserNavigationEnabled: Boolean,
    isZoomGesturesEnabled: Boolean,
    clickListeners: StreetViewPanoramaEventListeners
) {
    val streetViewPanorama =
        (currentComposer.applier as StreetViewPanoramaApplier).streetViewPanorama
    ComposeNode<StreetViewPanoramaPropertiesNode, StreetViewPanoramaApplier>(
        factory = {
            StreetViewPanoramaPropertiesNode(
                cameraPositionState = cameraPositionState,
                panorama = streetViewPanorama,
                eventListeners = clickListeners,
            )
        }
    ) {
        set(isPanningGesturesEnabled) {
            panorama.isPanningGesturesEnabled = isPanningGesturesEnabled
        }
        set(isStreetNamesEnabled) { panorama.isStreetNamesEnabled = isStreetNamesEnabled }
        set(isUserNavigationEnabled) {
            panorama.isUserNavigationEnabled = isUserNavigationEnabled
        }
        set(isZoomGesturesEnabled) { panorama.isZoomGesturesEnabled = isZoomGesturesEnabled }
        set(clickListeners) { this.eventListeners = it }
    }
}