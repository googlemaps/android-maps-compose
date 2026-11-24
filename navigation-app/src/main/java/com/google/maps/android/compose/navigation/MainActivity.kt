/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.maps.android.compose.navigation

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.navigation.NavigationApi
import com.google.maps.android.compose.navigation.ui.theme.MapsComposeNavigationTheme
import kotlinx.coroutines.launch

val defaultLocation = LatLng(40.014984, -105.270546)

class MainActivity : ComponentActivity() {
    private val myViewModel: NavigationViewModel by viewModels { NavigationViewModel.Factory }

    override fun onResume() {
        super.onResume()
        myViewModel.checkLocationPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NavigationApi.getNavigator(this, myViewModel)

        setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            val locationPermissionsLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions(),
            ) { permissions ->
                if (permissions.values.any { it }) {
                    myViewModel.checkLocationPermission()
                }
            }

            val location by myViewModel.location.collectAsStateWithLifecycle()
            val placesAlongRoute by myViewModel.placesAlongRoute.collectAsStateWithLifecycle()
            val routeReady by myViewModel.routeReady.collectAsStateWithLifecycle()
            val simulationState by myViewModel.simulationState.collectAsStateWithLifecycle()
            val simulationSpeed by myViewModel.simulationSpeed.collectAsStateWithLifecycle()

            fun onShowSnackbar(message: String) {
                scope.launch {
                    val duration = if (message.length > 40) {
                        SnackbarDuration.Long
                    } else {
                        SnackbarDuration.Short
                    }
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = duration,
                    )
                }
            }

            LaunchedEffect(myViewModel) {
                myViewModel.uiEvent.collect { event ->
                    when (event) {
                        is UiEvent.ShowSnackbar -> onShowSnackbar(event.message)

                        is UiEvent.RequestLocationPermission -> {
                            locationPermissionsLauncher.launch(
                                arrayOf(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION),
                            )
                        }
                    }
                }
            }

            MapsComposeNavigationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                ) { innerPadding ->
                    NavigationScreen(
                        modifier = Modifier.padding(innerPadding),
                        deviceLocation = location,
                        placesAlongRoute = placesAlongRoute,
                        routeReady = routeReady,
                        simulationState = simulationState,
                        simulationSpeed = simulationSpeed,
                        isSnackbarVisible = snackbarHostState.currentSnackbarData != null,
                        onClearSearchResults = {
                            myViewModel.clearSearchResults()
                        },
                        onSearchClicked = {
                            myViewModel.searchAlongRoute(it)
                        },
                        onDestinationSearchClicked = {
                            myViewModel.searchDestinationPlaces(it)
                        },
                        onSimulationSpeedChanged = { speed ->
                            myViewModel.setSimulationSpeed(speed)
                        },
                        onPauseSimulation = {
                            myViewModel.pauseSimulation()
                        },
                        onResumeSimulation = {
                            myViewModel.resumeSimulation()
                        },
                        onStopNavigation = {
                            myViewModel.stopNavigation()
                        },
                        onPlaceSelected = { place ->
                            myViewModel.navigateToSelectedPlace(place)
                        },
                        onMapDestinationSelected = { latLng ->
                            myViewModel.navigateToLatLng(latLng)
                        },
                    )
                }
            }
        }
    }
}
