package com.google.maps.android.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.delay
import kotlin.random.Random

private val mapListItems = List(100) { index ->
    val title = "Item #$index"
    val centerLat = Random.nextDouble(-50.0, 75.0)
    val centerLng = Random.nextDouble(-180.0, 180.0)
    val zoom = Random.nextDouble(0.0, 21.0).toFloat()
    MapListItem(title, LatLng(centerLat, centerLng), zoom)
}

class MapsInLazyColumnActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MapsInLazyColumn(mapListItems)
        }
    }
}

private data class MapListItem(
    val title: String,
    val location: LatLng,
    val zoom: Float
)

@Composable
private fun MapsInLazyColumn(items: List<MapListItem>) {
    LazyColumn {
        items(items) { item ->
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
                MyMap(
                    Modifier.fillMaxSize(),
                    item
                )
                Text(
                    item.title,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp))
            }
        }
    }
}

@Composable
private fun MyMap(
    modifier: Modifier,
    mapItem: MapListItem
) {
    val cameraPositionState = rememberCameraPositionState(init = { position = defaultCameraPosition })
    var mapLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(mapItem, mapLoaded) {
        if(!mapLoaded) return@LaunchedEffect

        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(mapItem.location, mapItem.zoom)
        cameraPositionState.move(cameraUpdate)
    }

    Box {
        GoogleMap(
            modifier = modifier,
            cameraPositionState = cameraPositionState,
            onMapLoaded = { mapLoaded = true }
        )
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
