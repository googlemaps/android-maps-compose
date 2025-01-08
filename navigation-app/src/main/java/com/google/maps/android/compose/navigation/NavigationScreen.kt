package com.google.maps.android.compose.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.navigation.NavigationView
import com.google.maps.android.compose.ComposeMapColorScheme
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.navigation.components.MovableMarker
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun NavigationScreen(
    deviceLocation: LatLng?,
    modifier: Modifier = Modifier
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            deviceLocation ?: defaultLocation,
            15f
        )
    }

    LaunchedEffect(deviceLocation) {
        cameraPositionState.animate(
            update = CameraUpdateFactory.newCameraPosition(
                CameraPosition.fromLatLngZoom(
                    deviceLocation ?: defaultLocation,
                    15f
                )
            )
        )
    }

    Column(
        modifier = modifier
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            mapColorScheme = ComposeMapColorScheme.FOLLOW_SYSTEM,
            mapViewCreator = { context, options ->
                NavigationViewDelegate(NavigationView(context, options))
            }
        ) {
            if (deviceLocation != null) {
                MovableMarker(
                    position = deviceLocation,
                    title = "User location",
                )
            }
        }
    }
}