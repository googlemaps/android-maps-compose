package com.google.maps.android.compose.streetview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.maps.model.StreetViewPanoramaCamera
import com.google.android.gms.maps.model.StreetViewPanoramaOrientation

/**
 * Holder class for top-level event listeners for [StreetViewPanorama].
 */
internal class StreetViewPanoramaEventListeners {
    var onClick: (StreetViewPanoramaOrientation) -> Unit by mutableStateOf({})
    var onLongClick: (StreetViewPanoramaOrientation) -> Unit by mutableStateOf({})
}