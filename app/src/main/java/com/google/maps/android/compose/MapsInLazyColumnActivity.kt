package com.google.maps.android.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.IndoorBuilding
import com.google.android.gms.maps.model.LatLng

private data class CountryLocation(val name: String, val latLng: LatLng, val zoom: Float)

// From https://developers.google.com/public-data/docs/canonical/countries_csv
private val countries = listOf(
    CountryLocation("Hong Kong", LatLng(22.396428, 114.109497), 5f),
    CountryLocation("Madison Square Garden (has indoor mode)", LatLng(40.7504656, -73.9937246), 19.33f),
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
    CountryLocation("Burundi", LatLng(-3.373056, 29.918886), 5f)
)

private data class MapListItem(
    val title: String,
    val location: LatLng,
    val zoom: Float,
    val id: String
)

private val allItems = countries.mapIndexed { index, country ->
    MapListItem(country.name, country.latLng, country.zoom, "MapInLazyColumn#$index")
}

class MapsInLazyColumnActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var showLazyColumn by rememberSaveable { mutableStateOf(true) }
            var visibleItems by rememberSaveable { mutableStateOf(allItems) }

            fun setItemCount(count: Int) {
                visibleItems = allItems.take(count.coerceIn(0, allItems.size))
            }

            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = { setItemCount(0) }) {
                        Text(text = "Clear")
                    }
                    TextButton(onClick = { setItemCount(visibleItems.size - 1) }) {
                        Text(text = "Remove")
                    }
                    TextButton(onClick = { showLazyColumn = !showLazyColumn }) {
                        Text(text = if(showLazyColumn) "Hide" else "Show")
                    }
                    TextButton(onClick = { setItemCount(visibleItems.size + 1) }) {
                        Text(text = "Add")
                    }
                    TextButton(onClick = { setItemCount(allItems.size) }) {
                        Text(text = "Fill")
                    }
                }
                if(showLazyColumn) {
                    Box(Modifier.border(1.dp, Color.LightGray.copy(0.5f))) {
                        MapsInLazyColumn(visibleItems)
                    }
                }
            }
        }
    }
}

@Composable
private fun MapsInLazyColumn(mapItems: List<MapListItem>) {
    LazyColumn {
        items(mapItems, key = { it.id }) { item ->
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

@OptIn(MapsComposeExperimentalApi::class)
@Composable
private fun MapCard(item: MapListItem) {
    Card(
        Modifier.padding(16.dp),
        elevation = 4.dp
    ) {
        var mapLoaded by remember { mutableStateOf(false) }
        var buildingFocused: Boolean? by remember { mutableStateOf(null) }
        var focusedBuildingInvocationCount by remember { mutableIntStateOf(0) }
        var activatedIndoorLevel: String? by remember { mutableStateOf(null) }
        var activatedIndoorLevelInvocationCount by remember { mutableIntStateOf(0) }
        var onMapClickCount by remember { mutableIntStateOf(0) }

        val cameraPositionState = rememberCameraPositionState(
            key = item.id,
            init = { position = CameraPosition.fromLatLngZoom(item.location, item.zoom) }
        )

        var map: GoogleMap? by remember { mutableStateOf(null) }

        fun updateIndoorLevel() {
            activatedIndoorLevel = map!!.focusedBuilding?.run { levels.getOrNull(activeLevelIndex)?.name }
        }

        Box {
            GoogleMap(
                onMapClick = {
                    onMapClickCount++
                },
                properties = remember {
                    MapProperties(
                        isBuildingEnabled = true,
                        isIndoorEnabled = true
                    )
                },
                cameraPositionState = cameraPositionState,
                onMapLoaded = { mapLoaded = true },
                indoorStateChangeListener = object : IndoorStateChangeListener {
                    override fun onIndoorBuildingFocused() {
                        super.onIndoorBuildingFocused()
                        focusedBuildingInvocationCount++
                        buildingFocused = (map!!.focusedBuilding != null)
                        updateIndoorLevel()
                    }

                    override fun onIndoorLevelActivated(building: IndoorBuilding) {
                        super.onIndoorLevelActivated(building)
                        activatedIndoorLevelInvocationCount++
                        updateIndoorLevel()
                    }
                }
            ) {
                MapEffect(Unit) {
                    map = it
                    updateIndoorLevel()
                }
            }

            AnimatedVisibility(!mapLoaded, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            @Composable
            fun TextWithBackground(text: String, fontWeight: FontWeight = FontWeight.Medium) {
                Text(
                    modifier = Modifier.background(Color.White.copy(0.7f)),
                    text = text,
                    fontWeight = fontWeight,
                    fontSize = 10.sp
                )
            }

            Column(
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                TextWithBackground(item.title, fontWeight = FontWeight.Bold)
                TextWithBackground("Map loaded: $mapLoaded")
                TextWithBackground("Map click count: $onMapClickCount")
                TextWithBackground("Building focused: $buildingFocused")
                TextWithBackground("Building focused invocation count: $focusedBuildingInvocationCount")
                TextWithBackground("Indoor level: $activatedIndoorLevel")
                TextWithBackground("Indoor level invocation count: $activatedIndoorLevelInvocationCount")
            }
        }
    }
}
