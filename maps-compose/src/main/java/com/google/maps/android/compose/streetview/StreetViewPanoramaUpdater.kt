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
        panorama.setOnStreetViewPanoramaCameraChangeListener {
            cameraPositionState.rawPanoramaCamera = it
        }
        panorama.setOnStreetViewPanoramaChangeListener {
            cameraPositionState.rawLocation = it
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