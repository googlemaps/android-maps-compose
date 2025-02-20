package com.google.maps.android.compose.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.navigation.NavigationView
import com.google.maps.android.compose.ComposeMapColorScheme
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.navigation.components.MovableMarker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

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

            MarkerComposable(
                title = "Bigfoot",
                 state = rememberMarkerState(position = LatLng(39.99932703674056, -105.28152457787887)),
            ) {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.bigfoot),
                        contentDescription = ""
                    )
                }
            }
        }
    }
}
