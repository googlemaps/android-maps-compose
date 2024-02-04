package com.google.maps.android.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng

class MapsInLazyColumnActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MapsInLazyColumn()
        }
    }
}

@Composable
private fun MapsInLazyColumn() {
    LazyColumn {
        items(mapListItems, key = { it.id }) { item ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                MapCard(item)
            }
        }
    }
}

@Composable
private fun MapCard(item: MapListItem) {
    Card(
        Modifier
            .padding(16.dp),
        elevation = 4.dp
    ) {
        Column {
            Box {
                CardMap(
                    Modifier.fillMaxSize(),
                    item
                )
                Text(
                    item.title,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(Color.White.copy(0.8f))
                )
            }
        }
    }
}

@Composable
private fun CardMap(
    modifier: Modifier,
    mapItem: MapListItem
) {
    var mapLoaded by remember { mutableStateOf(false) }
    val cameraPositionState = rememberCameraPositionState(
        key = mapItem.id,
        init = { position = CameraPosition.fromLatLngZoom(mapItem.location, mapItem.zoom) }
    )

    Box {
        GoogleMap(
            modifier = modifier,
            cameraPositionState = cameraPositionState,
            onMapLoaded = { mapLoaded = true }
        ) {
            Marker(state = rememberMarkerState(position = mapItem.location))
        }

        AnimatedVisibility(!mapLoaded, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

private data class MapListItem(
    val title: String,
    val location: LatLng,
    val zoom: Float,
    val id: String
)

// From https://developers.google.com/public-data/docs/canonical/countries_csv
private val countries = listOf(
    CountryLocation("Hong Kong", LatLng(22.396428, 114.109497), 5f),
    CountryLocation("Bolivia", LatLng(-16.290154, -63.588653), 5f),
    CountryLocation("Ecuador", LatLng(-1.831239, -78.183406), 5f),
    CountryLocation("Sweden", LatLng(60.128161, 18.643501), 5f),
    CountryLocation("Eritrea", LatLng(15.179384, 39.782334), 5f),
    CountryLocation("Portugal", LatLng(39.399872, -8.224454), 5f),
    CountryLocation("Belgium", LatLng(50.503887, 4.469936), 5f),
    CountryLocation("Slovakia", LatLng(48.669026, 19.699024), 5f),
    CountryLocation("El Salvador", LatLng(13.794185, -88.89653), 5f),
    CountryLocation("Bhutan", LatLng(27.514162, 90.433601), 5f),
    CountryLocation("Saint Lucia", LatLng(13.909444, -60.978893), 5f),
    CountryLocation("Uganda", LatLng(1.373333, 32.290275), 5f),
    CountryLocation("South Africa", LatLng(-30.559482, 22.937506), 5f),
    CountryLocation("Spain", LatLng(40.463667, -3.74922), 5f),
    CountryLocation("Georgia", LatLng(42.315407, 43.356892), 5f),
    CountryLocation("Burundi", LatLng(-3.373056, 29.918886), 5f),
    CountryLocation("Christmas Island", LatLng(-10.447525, 105.690449), 5f),
    CountryLocation("Vanuatu", LatLng(-15.376706, 166.959158), 5f),
    CountryLocation("Jersey", LatLng(49.214439, -2.13125), 5f),
    CountryLocation("Svalbard and Jan Mayen", LatLng(77.553604, 23.670272), 5f),
    CountryLocation("American Samoa", LatLng(-14.270972, -170.132217), 5f),
    CountryLocation("Moldova", LatLng(47.411631, 28.369885), 5f),
    CountryLocation("Bouvet Island", LatLng(-54.423199, 3.413194), 5f),
    CountryLocation("Puerto Rico", LatLng(18.220833, -66.590149), 5f),
    CountryLocation("Colombia", LatLng(4.570868, -74.297333), 5f),
)

private val mapListItems = countries
    .mapIndexed { index, country ->
    MapListItem(country.name, country.latLng, country.zoom, "MapInLazyColumn#$index")
}

private data class CountryLocation(val name: String, val latLng: LatLng, val zoom: Float)