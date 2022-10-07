package com.google.maps.android.compose.streetview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.maps.model.StreetViewPanoramaOrientation

/**
 * Holderc lass for top-level click listeners for [StreetViewPanorama].
 */
internal class StreetViewPanoramaClickListeners {
    var onClick: (StreetViewPanoramaOrientation) -> Unit by mutableStateOf({})
    var onLongClick: (StreetViewPanoramaOrientation) -> Unit by mutableStateOf({})
}