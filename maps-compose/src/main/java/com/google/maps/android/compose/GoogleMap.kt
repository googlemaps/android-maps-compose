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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
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
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.PointOfInterest
import com.google.maps.android.ktx.awaitMap
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * A compose container for a [MapView].
 *
 * @param modifier - Modifier to be applied to the GoogleMap
 * @param cameraPositionState - the [CameraPositionState] to be used to control or observe the map's
 * camera state
 * @param contentDescription - the content description for the map used by accessibility services to
 * describe the map.
 * @param googleMapOptionsFactory - the block for creating the [GoogleMapOptions] provided when the
 * map is created
 * @param isBuildingEnabled - boolean indicating if buildings are enabled
 * @param isIndoorEnabled - boolean indicating if indoor maps are enabled
 * @param isMyLocationEnabled - boolean indicating if the my-location layer should be enabled. Before
 * setting this property to 'true', ensure that `ACCESS_COARSE_LOCATION` or `ACCESS_FINE_LOCATION`
 * permissions have been granted.
 * @param isTrafficEnabled - boolean indicating if the traffic layer is on or off.
 * @param latLngBoundsForCameraTarget - a [LatLngBounds] to constrain the camera target.
 * @param locationSource - the [LocationSource] to be used to provide location data
 * @param mapStyleOptions - the styling options for the map
 * @param mapType - the type of the map tiles that should be displayed
 * @param maxZoomPreference - the preferred upper bound for the camera zoom.
 * @param minZoomPreference - the preferred lower bound for the camera zoom.
 * @param uiSettings - the [MapUiSettings] to be used for UI-specific settings on the map
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
    cameraPositionState: CameraPositionState = rememberCameraPositionState(),
    contentDescription: String? = "Google Map",
    googleMapOptionsFactory: () -> GoogleMapOptions = { GoogleMapOptions() },
    isBuildingEnabled: Boolean = false,
    isIndoorEnabled: Boolean = false,
    isMyLocationEnabled: Boolean = false,
    isTrafficEnabled: Boolean = false,
    latLngBoundsForCameraTarget: LatLngBounds? = null,
    locationSource: LocationSource? = null,
    mapStyleOptions: MapStyleOptions? = null,
    mapType: MapType = MapType.NORMAL,
    maxZoomPreference: Float = 21.0f,
    minZoomPreference: Float = 3.0f,
    uiSettings: MapUiSettings = MapUiSettings(),
    onIndoorBuildingFocused: () -> Unit = {},
    onIndoorLevelActivated: (IndoorBuilding) -> Unit = {},
    onMapClick: (LatLng) -> Unit = {},
    onMapLongClick: (LatLng) -> Unit = {},
    onMapLoaded: () -> Unit = {},
    onMyLocationButtonClick: () -> Boolean = { false },
    onMyLocationClick: () -> Unit = {},
    onPOIClick: (PointOfInterest) -> Unit = {},
    contentPadding: PaddingValues = NoPadding,
    content: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context, googleMapOptionsFactory()) }

    AndroidView(modifier = modifier, factory = { mapView })
    MapLifecycle(mapView)

    // rememberUpdatedState and friends are used here to make these values observable to
    // the subcomposition without providing a new content function each recomposition
    val mapClickListeners = remember { MapClickListeners() }.also {
        it.onIndoorBuildingFocused = onIndoorBuildingFocused
        it.onIndoorLevelActivated = onIndoorLevelActivated
        it.onMapClick = onMapClick
        it.onMapLongClick = onMapLongClick
        it.onMapLoaded = onMapLoaded
        it.onMyLocationButtonClick = onMyLocationButtonClick
        it.onMyLocationClick = onMyLocationClick
        it.onPOIClick = onPOIClick
    }
    val mapPropertiesHolder = remember { MapPropertiesHolder() }.also {
        it.contentDescription = contentDescription
        it.isBuildingEnabled = isBuildingEnabled
        it.isIndoorEnabled = isIndoorEnabled
        it.isMyLocationEnabled = isMyLocationEnabled
        it.isTrafficEnabled = isTrafficEnabled
        it.latLngBoundsForCameraTarget = latLngBoundsForCameraTarget
        it.mapStyleOptions = mapStyleOptions
        it.mapType = mapType
        it.maxZoomPreference = maxZoomPreference
        it.minZoomPreference = minZoomPreference
    }
    val currentLocationSource by rememberUpdatedState(locationSource)
    val currentCameraPositionState by rememberUpdatedState(cameraPositionState)
    val currentContentPadding by rememberUpdatedState(contentPadding)
    val currentUiSettings by rememberUpdatedState(uiSettings)

    val parentComposition = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    LaunchedEffect(Unit) {
        val map = mapView.awaitMap()
        disposingComposition {
            map.newComposition(parentComposition) {
                MapProperties(
                    cameraPositionState = currentCameraPositionState,
                    clickListeners = mapClickListeners,
                    contentPadding = currentContentPadding,
                    locationSource = currentLocationSource,
                    mapPropertiesHolder = mapPropertiesHolder,
                    mapUiSettings = currentUiSettings,
                )

                currentContent?.invoke()
            }
        }
    }
}

private suspend inline fun disposingComposition(factory: () -> Composition) {
    val composition = factory()
    try {
        awaitCancellation()
    } finally {
        composition.dispose()
    }
}

private fun GoogleMap.newComposition(
    parent: CompositionContext,
    content: @Composable () -> Unit
): Composition = Composition(MapApplier(this), parent).apply {
    setContent(content)
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
    object : ComponentCallbacks {
        override fun onConfigurationChanged(config: Configuration) {}

        override fun onLowMemory() {
            this@componentCallbacks.onLowMemory()
        }
    }
