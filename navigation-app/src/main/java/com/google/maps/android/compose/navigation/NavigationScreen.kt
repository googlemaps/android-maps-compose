package com.google.maps.android.compose.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
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
import com.google.android.libraries.places.api.model.Place
import com.google.maps.android.compose.ComposeMapColorScheme
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.navigation.components.MovableMarker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

@Composable
fun NavigationScreen(
    deviceLocation: LatLng?,
    placesAlongRoute: List<Place>,
    onClearSearchResults: () -> Unit,
    onSearchClicked: (String) -> Unit,
    routeReady: Boolean,
    modifier: Modifier = Modifier,
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
        if (placesAlongRoute.isNotEmpty()) {
            OutlinedCard(
                modifier = Modifier.height(250.dp).fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.padding(16.dp),
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(placesAlongRoute) { place ->
                            PlaceItem(place)
                        }
                    }

                    IconButton(
                        onClick = onClearSearchResults,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
            }
        } else if (routeReady) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onSearchClicked("Spicy Vegetarian Food")
                        }
                    ) {
                        Text("Spicy Veg")
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onSearchClicked("Pizza")
                        }
                    ) {
                        Text("Pizza")
                    }
                }
            }
        }

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

@Composable
fun PlaceItem(place: Place) {
    // Should probably filter these.  I would not use the place object directly.
    Text(place.displayName ?: "Unknown place")
}
