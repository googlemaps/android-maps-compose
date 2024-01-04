// Copyright 2023 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.


package com.google.maps.android.compose

import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.LocationSource
import com.google.android.gms.maps.LocationSource.OnLocationChangedListener
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import kotlin.random.Random

private const val TAG = "LocationTrackActivity"
private const val zoom = 8f

/**
 * This shows how to use a custom location source to show a blue dot on the map based on your own
 * locations.
 */
class LocationTrackingActivity : AppCompatActivity() {

    private val locationSource = MyLocationSource()
    private var counter = 0

    // Generates "fake" locations randomly every 2 seconds.
    // Normally you'd request location updates:
    // https://developer.android.com/training/location/request-updates
    private val locationFlow = callbackFlow {
        while (true) {
            ++counter

            val location = newLocation()
            Log.d(TAG, "Location $counter: $location")
            trySend(location)

            delay(2_000)
        }
    }.shareIn(
        lifecycleScope,
        replay = 0,
        started = SharingStarted.WhileSubscribed()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var isMapLoaded by remember { mutableStateOf(false) }

            // To control and observe the map camera
            val cameraPositionState = rememberCameraPositionState {
                position = defaultCameraPosition
            }

            // To show blue dot on map
            val mapProperties by remember { mutableStateOf(MapProperties(isMyLocationEnabled = true)) }

            // Collect location updates
            val locationState = locationFlow.collectAsState(initial = newLocation())

            // Update blue dot and camera when the location changes
            LaunchedEffect(locationState.value) {
                Log.d(TAG, "Updating blue dot on map...")
                locationSource.onLocationChanged(locationState.value)

                Log.d(TAG, "Updating camera position...")
                val cameraPosition = CameraPosition.fromLatLngZoom(LatLng(locationState.value.latitude, locationState.value.longitude), zoom)
                cameraPositionState.animate(CameraUpdateFactory.newCameraPosition(cameraPosition), 1_000)
            }

            // Detect when the map starts moving and print the reason
            LaunchedEffect(cameraPositionState.isMoving) {
                if (cameraPositionState.isMoving) {
                    Log.d(TAG, "Map camera started moving due to ${cameraPositionState.cameraMoveStartedReason.name}")
                }
            }

            Box(Modifier.fillMaxSize()) {
                GoogleMap(
                    modifier = Modifier.matchParentSize(),
                    cameraPositionState = cameraPositionState,
                    onMapLoaded = {
                        isMapLoaded = true
                    },
                    // This listener overrides the behavior for the location button. It is intended to be used when a
                    // custom behavior is needed.
                    onMyLocationButtonClick = {  Log.d(TAG,"Overriding the onMyLocationButtonClick with this Log"); true },
                    locationSource = locationSource,
                    properties = mapProperties
                )
                if (!isMapLoaded) {
                    AnimatedVisibility(
                        modifier = Modifier
                            .matchParentSize(),
                        visible = !isMapLoaded,
                        enter = EnterTransition.None,
                        exit = fadeOut()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .background(MaterialTheme.colors.background)
                                .wrapContentSize()
                        )
                    }
                }
            }
        }
    }
}

/**
 * A [LocationSource] which allows it's location to be set. In practice you would request location
 * updates (https://developer.android.com/training/location/request-updates).
 */
private class MyLocationSource : LocationSource {

    private var listener: OnLocationChangedListener? = null

    override fun activate(listener: OnLocationChangedListener) {
        this.listener = listener
    }

    override fun deactivate() {
        listener = null
    }

    fun onLocationChanged(location: Location) {
        listener?.onLocationChanged(location)
    }
}

private fun newLocation(): Location {
    val location = Location("MyLocationProvider")
    location.apply {
        latitude = singapore.latitude + Random.nextFloat()
        longitude = singapore.longitude + Random.nextFloat()
    }
    return location
}