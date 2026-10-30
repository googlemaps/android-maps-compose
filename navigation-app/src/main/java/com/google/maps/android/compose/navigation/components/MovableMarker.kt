package com.google.maps.android.compose.navigation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState

@Composable
fun MovableMarker(position: LatLng, title: String? = null, snippet: String? = null) {
    val state = rememberUpdatedMarkerState(position)
    Marker(
        state = state,
        title = title,
        snippet = snippet,
    )
}

@Composable
fun rememberUpdatedMarkerState(newPosition: LatLng) =
    remember { MarkerState(position = newPosition) }.apply { position = newPosition }
