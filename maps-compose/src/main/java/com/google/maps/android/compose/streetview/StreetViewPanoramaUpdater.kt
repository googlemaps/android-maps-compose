package com.google.maps.android.compose.streetview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.currentComposer
import com.google.android.gms.maps.StreetViewPanorama
import com.google.maps.android.compose.MapNode

internal class StreetViewPanoramaPropertiesNode(
    val panorama: StreetViewPanorama,
    var clickListeners: StreetViewPanoramaClickListeners,
) : MapNode {
    override fun onAttached() {
        super.onAttached()
        panorama.setOnStreetViewPanoramaClickListener {
            clickListeners.onClick(it)
        }
        panorama.setOnStreetViewPanoramaLongClickListener {
            clickListeners.onLongClick(it)
        }
    }
}

/**
 * Used to keep the street view panorama properties up-to-date.
 */
@Suppress("NOTHING_TO_INLINE")
@Composable
internal inline fun StreetViewUpdater(
    clickListeners: StreetViewPanoramaClickListeners
) {
    val streetViewPanorama =
        (currentComposer.applier as StreetViewPanoramaApplier).streetViewPanorama
    ComposeNode<StreetViewPanoramaPropertiesNode, StreetViewPanoramaApplier>(
        factory = {
            StreetViewPanoramaPropertiesNode(
                panorama = streetViewPanorama,
                clickListeners = clickListeners,
            )
        }
    ) {
        set(clickListeners) { this.clickListeners = it }
    }
}