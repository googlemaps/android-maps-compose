package com.google.maps.android.compose.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.navigation.NavigationView
import com.google.android.libraries.places.api.model.Place
import com.google.maps.android.compose.ComposeMapColorScheme
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.navigation.components.MovableMarker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.delay

internal const val CONTROLS_AUTO_COLLAPSE_DELAY_MS = 4_000L

@Composable
fun NavigationScreen(
    deviceLocation: LatLng?,
    placesAlongRoute: List<Place>,
    onClearSearchResults: () -> Unit,
    onSearchClicked: (String) -> Unit,
    routeReady: Boolean,
    modifier: Modifier = Modifier,
    simulationState: SimulationState = SimulationState.IDLE,
    simulationSpeed: Float = DEFAULT_SIMULATION_SPEED,
    destinationLabel: String = DEFAULT_DESTINATION_NAME,
    onSimulationSpeedChanged: (Float) -> Unit = {},
    onPauseSimulation: () -> Unit = {},
    onResumeSimulation: () -> Unit = {},
    onStopNavigation: () -> Unit = {},
    onDestinationSearchClicked: (String) -> Unit = onSearchClicked,
    onPlaceSelected: (Place) -> Unit = {},
    onMapDestinationSelected: (LatLng) -> Unit = {},
) {
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchAlongRouteMode by rememberSaveable { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            deviceLocation ?: defaultLocation,
            15f,
        )
    }

    LaunchedEffect(deviceLocation, simulationState) {
        if (simulationState != SimulationState.RUNNING) {
            deviceLocation?.let { location ->
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newCameraPosition(
                        CameraPosition.fromLatLngZoom(
                            location,
                            15f,
                        ),
                    ),
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            mapColorScheme = ComposeMapColorScheme.FOLLOW_SYSTEM,
            onMapLongClick = onMapDestinationSelected,
            mapViewCreator = { context, options ->
                NavigationViewDelegate(NavigationView(context, options))
            },
        ) {
            if (deviceLocation != null) {
                MovableMarker(
                    position = deviceLocation,
                    title = "User location",
                )
            }

            placesAlongRoute.forEach { place ->
                val placeLocation = place.location
                if (placeLocation != null) {
                    Marker(
                        state = rememberUpdatedMarkerState(position = placeLocation),
                        title = place.displayName ?: "Place",
                        snippet = place.formattedAddress,
                    )
                }
            }

            MarkerComposable(
                title = "Bigfoot",
                state = rememberUpdatedMarkerState(position = LatLng(39.99932703674056, -105.28152457787887)),
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
                        contentDescription = "",
                    )
                }
            }
        }

        if (isSearchExpanded || placesAlongRoute.isNotEmpty()) {
            FloatingPlaceSearchCard(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                routeReady = routeReady,
                searchAlongRouteMode = searchAlongRouteMode && routeReady,
                onSearchModeChange = { searchAlongRouteMode = it },
                places = placesAlongRoute,
                onSubmitSearch = {
                    val trimmed = searchQuery.trim()
                    if (trimmed.isNotEmpty()) {
                        if (searchAlongRouteMode && routeReady) {
                            onSearchClicked(trimmed)
                        } else {
                            onDestinationSearchClicked(trimmed)
                        }
                    }
                },
                onPlaceSelected = { place ->
                    isSearchExpanded = false
                    onPlaceSelected(place)
                },
                onClose = {
                    isSearchExpanded = false
                    onClearSearchResults()
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            )
        }

        CollapsibleFloatingControls(
            routeReady = routeReady,
            simulationState = simulationState,
            simulationSpeed = simulationSpeed,
            onOpenSearch = {
                searchAlongRouteMode = routeReady
                isSearchExpanded = true
            },
            onSimulationSpeedChanged = onSimulationSpeedChanged,
            onPauseSimulation = onPauseSimulation,
            onResumeSimulation = onResumeSimulation,
            onStopNavigation = onStopNavigation,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(horizontal = 12.dp),
        )
    }
}

@Composable
private fun FloatingPlaceSearchCard(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    routeReady: Boolean,
    searchAlongRouteMode: Boolean,
    onSearchModeChange: (Boolean) -> Unit,
    places: List<Place>,
    onSubmitSearch: () -> Unit,
    onPlaceSelected: (Place) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = {
                        Text(
                            if (searchAlongRouteMode) {
                                "Search along active route..."
                            } else {
                                "Search any destination..."
                            },
                        )
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSubmitSearch() }),
                )
                Button(
                    onClick = onSubmitSearch,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text("Find")
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            if (routeReady) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = !searchAlongRouteMode,
                        onClick = { onSearchModeChange(false) },
                        label = { Text("New Destination") },
                    )
                    FilterChip(
                        selected = searchAlongRouteMode,
                        onClick = { onSearchModeChange(true) },
                        label = { Text("Along Route") },
                    )
                }
            }

            if (places.isNotEmpty()) {
                HorizontalDivider()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(places) { place ->
                        PlaceItem(
                            place = place,
                            onNavigateClicked = { onPlaceSelected(place) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Floating control menu positioned along the mid-right edge of the screen so it never obscures
 * the top turn-by-turn guidance banner or the bottom trip progress bar.
 *
 * Automatically collapses into a single compact FAB after [CONTROLS_AUTO_COLLAPSE_DELAY_MS] of
 * inactivity.
 */
@Composable
private fun CollapsibleFloatingControls(
    routeReady: Boolean,
    simulationState: SimulationState,
    simulationSpeed: Float,
    onOpenSearch: () -> Unit,
    onSimulationSpeedChanged: (Float) -> Unit,
    onPauseSimulation: () -> Unit,
    onResumeSimulation: () -> Unit,
    onStopNavigation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    var speedMenuExpanded by remember { mutableStateOf(false) }
    var interactionTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(expanded, speedMenuExpanded, interactionTick) {
        if (expanded && !speedMenuExpanded) {
            delay(CONTROLS_AUTO_COLLAPSE_DELAY_MS)
            expanded = false
        }
    }

    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = !expanded,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            SmallFloatingActionButton(
                onClick = {
                    interactionTick++
                    expanded = true
                },
                shape = CircleShape,
            ) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Expand controls",
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            interactionTick++
                            expanded = false
                            onOpenSearch()
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search places",
                        )
                        Text(
                            text = "Search",
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }

                    Box {
                        OutlinedButton(
                            onClick = {
                                interactionTick++
                                speedMenuExpanded = true
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text("${simulationSpeed.toInt()}x")
                        }
                        DropdownMenu(
                            expanded = speedMenuExpanded,
                            onDismissRequest = {
                                interactionTick++
                                speedMenuExpanded = false
                            },
                        ) {
                            SIMULATION_SPEED_OPTIONS.forEach { speed ->
                                val label = "${speed.toInt()}x"
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        interactionTick++
                                        speedMenuExpanded = false
                                        onSimulationSpeedChanged(speed)
                                    },
                                )
                            }
                        }
                    }

                    if (simulationState == SimulationState.RUNNING) {
                        OutlinedButton(
                            onClick = {
                                interactionTick++
                                onPauseSimulation()
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text("Pause")
                        }
                    } else if (simulationState == SimulationState.PAUSED || routeReady) {
                        OutlinedButton(
                            onClick = {
                                interactionTick++
                                onResumeSimulation()
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text("Resume")
                        }
                    }

                    if (routeReady || simulationState == SimulationState.RUNNING ||
                        simulationState == SimulationState.PAUSED
                    ) {
                        Button(
                            onClick = {
                                interactionTick++
                                onStopNavigation()
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text("Stop")
                        }
                    }

                    IconButton(
                        onClick = { expanded = false },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Collapse controls",
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlaceItem(place: Place, onNavigateClicked: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onNavigateClicked)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = place.displayName ?: "Unknown place",
                style = MaterialTheme.typography.bodyMedium,
            )
            place.formattedAddress?.let { address ->
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        TextButton(onClick = onNavigateClicked) {
            Text("Navigate")
        }
    }
}
