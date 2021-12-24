// Copyright 2021 Google LLC
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

import android.content.ComponentCallbacks
import android.content.res.Configuration
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.LocationSource
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.IndoorBuilding
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.google.maps.android.ktx.awaitMap
import kotlinx.coroutines.awaitCancellation

/**
 * A compose container for a [MapView].
 *
 * @param modifier - Modifier to be applied to the GoogleMap
 * @param googleMapOptions - GoogleMapOptions to be applied to the MapView when instantiated
 * @param mapPropertiesState - the [MapPropertiesState] to be used to set properties of the map
 * @param cameraPositionState - the [CameraPositionState] to be used to control or observe the map's
 * camera state
 * @param locationSource - the [LocationSource] to be used to provide location data
 * @param onIndoorBuildingFocused - lambda to be invoked when an indoor building is focused
 * @param onIndoorLevelActivated - lambda to be invoked when an level is activated in an indoor
 * building
 * @param onMapClick - lambda invoked when the map is clicked
 * @param onMapLoaded - lambda invoked when the map is finished loading
 * @param onMyLocationButtonClick - lambda invoked when the my location button is clicked
 * @param onMyLocationClick - lambda invoked when the my location dot is clicked
 * @param onPOIClick - lambda invoked when a POI is clicked
 * @param content - the content of the map
 */
@Composable
fun GoogleMap(
    modifier: Modifier = Modifier,
    googleMapOptions: GoogleMapOptions = GoogleMapOptions(), // FIXME
    uiSettingsState: UISettingsState = rememberUISettingsState(),
    mapPropertiesState: MapPropertiesState = rememberMapPropertiesState(),
    cameraPositionState: CameraPositionState = rememberCameraPositionState(),
    locationSource: LocationSource? = null,
    onIndoorBuildingFocused: () -> Unit = {},
    onIndoorLevelActivated: (IndoorBuilding) -> Unit = {},
    onMapClick: (LatLng) -> Unit = {},
    onMapLongClick: (LatLng) -> Unit = {},
    onMapLoaded: () -> Unit = {},
    onMyLocationButtonClick: () -> Boolean = { false },
    onMyLocationClick: () -> Unit = {},
    onPOIClick: (PointOfInterest) -> Unit = {},
    content: (@Composable GoogleMapScope.() -> Unit)? = null,
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context, googleMapOptions)
    }

    AndroidView(
        modifier = modifier,
        factory = {
            mapView
        }
    )

    MapLifecycle(mapView)
    UISettings(mapView, uiSettingsState)
    MapProperties(mapView, mapPropertiesState, locationSource)
    CameraEffect(mapView, cameraPositionState)

    // Click handlers
    LaunchedEffect(Unit) {
        val map = mapView.awaitMap()
        map.setOnMapClickListener { onMapClick(it) }
        map.setOnMapLongClickListener { onMapLongClick(it) }
        map.setOnMapLoadedCallback { onMapLoaded() }
        map.setOnMyLocationButtonClickListener { onMyLocationButtonClick() }
        map.setOnMyLocationClickListener { onMyLocationClick() }
        map.setOnPoiClickListener { onPOIClick(it) }
        map.setOnIndoorStateChangeListener(object : GoogleMap.OnIndoorStateChangeListener {
            override fun onIndoorBuildingFocused() {
                onIndoorBuildingFocused()
            }

            override fun onIndoorLevelActivated(building: IndoorBuilding) {
                onIndoorLevelActivated(building)
            }
        })
    }

    // Child APIs
    if (content != null) {
        val compositionContext = rememberCompositionContext()
        val currentContent by rememberUpdatedState(content)
        LaunchedEffect(Unit) {
            val map = mapView.awaitMap()
            val mapApplier = MapApplier(map)
            val composition = Composition(mapApplier, compositionContext)
            composition.setContent {
                GoogleMapScopeImpl().currentContent()
            }
            try {
                awaitCancellation()
            } finally {
                composition.dispose()
            }
        }
    }
}

/**
 * Registers lifecycle observers to the local [MapView].
 */
@Composable
private fun MapLifecycle(mapView: MapView) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(context, lifecycle, mapView) {
        val mapLifecycleObserver = mapView.lifecycleObserver()
        val callbacks = mapView.componentCallbacks()

        lifecycle.addObserver(mapLifecycleObserver)
        context.registerComponentCallbacks(callbacks)

        onDispose {
            lifecycle.removeObserver(mapLifecycleObserver)
            context.unregisterComponentCallbacks(callbacks)
        }
    }
}

private fun MapView.lifecycleObserver(): LifecycleEventObserver =
    LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_CREATE -> this.onCreate(Bundle())
            Lifecycle.Event.ON_START -> this.onStart()
            Lifecycle.Event.ON_RESUME -> this.onResume()
            Lifecycle.Event.ON_PAUSE -> this.onPause()
            Lifecycle.Event.ON_STOP -> this.onStop()
            Lifecycle.Event.ON_DESTROY -> this.onDestroy()
            else -> throw IllegalStateException()
        }
    }

private fun MapView.componentCallbacks(): ComponentCallbacks =
    object: ComponentCallbacks {
        override fun onConfigurationChanged(config: Configuration) { }

        override fun onLowMemory() {
            this@componentCallbacks.onLowMemory()
        }
    }
