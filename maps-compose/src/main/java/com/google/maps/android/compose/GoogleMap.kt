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

import android.content.ComponentCallbacks
import android.content.res.Configuration
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReusableComposition
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.LocationSource
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.google.maps.android.ktx.awaitMap
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch

internal const val TAG = "GoogleMap"

private var compositionCounter = 0
private var androidViewCounter = 0

/**
 * A compose container for a [MapView].
 *
 * @param mergeDescendants deactivates the map for accessibility purposes
 * @param modifier Modifier to be applied to the GoogleMap
 * @param cameraPositionState the [CameraPositionState] to be used to control or observe the map's
 * camera state
 * @param contentDescription the content description for the map used by accessibility services to
 * describe the map. If none is specified, the default is "Google Map".
 * @param googleMapOptionsFactory the block for creating the [GoogleMapOptions] provided when the
 * map is created
 * @param properties the properties for the map
 * @param locationSource the [LocationSource] to be used to provide location data
 * @param uiSettings the [MapUiSettings] to be used for UI-specific settings on the map
 * @param indoorStateChangeListener listener for indoor building state changes
 * @param onMapClick lambda invoked when the map is clicked
 * @param onMapLoaded lambda invoked when the map is finished loading
 * @param onMyLocationButtonClick lambda invoked when the my location button is clicked
 * @param onMyLocationClick lambda invoked when the my location dot is clicked
 * @param onPOIClick lambda invoked when a POI is clicked
 * @param contentPadding the padding values used to signal that portions of the map around the edges
 * may be obscured. The map will move the Google logo, etc. to avoid overlapping the padding.
 * @param content the content of the map
 */
@Composable
public fun GoogleMap(
    mergeDescendants: Boolean = false,
    modifier: Modifier = Modifier,
    cameraPositionState: CameraPositionState = rememberCameraPositionState(),
    contentDescription: String? = null,
    googleMapOptionsFactory: () -> GoogleMapOptions = { GoogleMapOptions() },
    properties: MapProperties = DefaultMapProperties,
    locationSource: LocationSource? = null,
    uiSettings: MapUiSettings = DefaultMapUiSettings,
    indoorStateChangeListener: IndoorStateChangeListener = DefaultIndoorStateChangeListener,
    onMapClick: ((LatLng) -> Unit)? = null,
    onMapLongClick: ((LatLng) -> Unit)? = null,
    onMapLoaded: (() -> Unit)? = null,
    onMyLocationButtonClick: (() -> Boolean)? = null,
    onMyLocationClick: ((Location) -> Unit)? = null,
    onPOIClick: ((PointOfInterest) -> Unit)? = null,
    contentPadding: PaddingValues = NoPadding,
    content: (@Composable @GoogleMapComposable () -> Unit)? = null,
) {
    // When in preview, early return a Box with the received modifier preserving layout
    if (LocalInspectionMode.current) {
        Box(modifier = modifier)
        return
    }

    // rememberUpdatedState and friends are used here to make these values observable to
    // the subcomposition without providing a new content function each recomposition
    var mapClickListeners by remember { mutableStateOf<MapClickListeners?>(null) }
    mapClickListeners?.also {
        it.indoorStateChangeListener = indoorStateChangeListener
        it.onMapClick = onMapClick
        it.onMapLongClick = onMapLongClick
        it.onMapLoaded = onMapLoaded
        it.onMyLocationButtonClick = onMyLocationButtonClick
        it.onMyLocationClick = onMyLocationClick
        it.onPOIClick = onPOIClick
    }

    val currentContentDescription by rememberUpdatedState(contentDescription)
    val currentLocationSource by rememberUpdatedState(locationSource)
    val currentCameraPositionState by rememberUpdatedState(cameraPositionState)
    val currentContentPadding by rememberUpdatedState(contentPadding)
    val currentUiSettings by rememberUpdatedState(uiSettings)
    val currentMapProperties by rememberUpdatedState(properties)

    val parentComposition = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    val mapUpdaterScope = rememberCoroutineScope()

    var composition by remember { mutableStateOf<ReusableComposition?>(null) }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current
    var mapLifecycleController: MapViewLifecycleController? by remember { mutableStateOf(null) }
    var componentCallbacks: ComponentCallbacks? by remember { mutableStateOf(null) }
    var isCompositionSet by remember { mutableStateOf(false) }

    // Debug stuff
    var debugMapReused: Boolean? by remember { mutableStateOf(null) }
    val debugCompositionId = remember { compositionCounter++ }
    var debugMapId: Int? by remember { mutableStateOf(null) }

    suspend fun MapView.getOrCreateComposition(
        mapClickListeners: MapClickListeners
    ): Pair<ReusableComposition, /* is reused */ Boolean> {
        val current = getTag(R.id.maps_compose_map_view_tag_composition) as? ReusableComposition

        if(current == null) {
            setTag(R.id.maps_compose_map_view_tag_debug_id, androidViewCounter++)
        }

        return if(current == null || current.isDisposed) {
            val map = awaitMap()
            ReusableComposition(
                MapApplier(map, this, mapClickListeners), parentComposition
            ).also { composition ->
                setTag(R.id.maps_compose_map_view_tag_composition, composition)
            } to false
        } else {
            current to true
        }.also { (_, reused) ->
            log("getOrCreateComposition. Reused composition: $reused.")
        }
    }

    fun MapView.getOrCreateMapListenersTag(): MapClickListeners {
        return getTag(R.id.maps_compose_map_view_tag_click_listeners) as? MapClickListeners ?: MapClickListeners()
            .also { clickListeners ->
                setTag(R.id.maps_compose_map_view_tag_click_listeners, clickListeners)
            }
    }

    /** Apply the [content] compositions to the map. */
    suspend fun setComposition(mapView: MapView) {
        mapView.log("setComposition")
        val clickListeners = mapView.getOrCreateMapListenersTag()
        val (currentComposition, mapReused) = mapView.getOrCreateComposition(clickListeners)

        composition = currentComposition

        val mapCompositionContent: @Composable () -> Unit = {
            MapUpdater(
                mergeDescendants = mergeDescendants,
                contentDescription = currentContentDescription,
                cameraPositionState = currentCameraPositionState,
                contentPadding = currentContentPadding,
                locationSource = currentLocationSource,
                mapProperties = currentMapProperties,
                mapUiSettings = currentUiSettings,
            )

            MapClickListenerUpdater()

            CompositionLocalProvider(
                LocalCameraPositionState provides currentCameraPositionState,
            ) {
                currentContent?.invoke()
            }
        }

        currentComposition.apply {
            if(mapReused) {
                setContentWithReuse(mapCompositionContent)
            } else {
                setContent(mapCompositionContent)
            }
        }

        // Set this after composition is started.
        mapClickListeners = clickListeners
    }

    Box {
        AndroidView(
            modifier = modifier,
            factory = {
                Log.d(TAG, "Factory")
                debugMapReused = false
                MapView(context, googleMapOptionsFactory()).also { mapView ->
                    mapLifecycleController = MapViewLifecycleController(
                        isMapReused = false,
                        mapView = mapView
                    )
                    componentCallbacks = mapView.componentCallbacks()
                }
            },
            onReset = { mapView ->
                mapView.log("onReset")
                // Deactivate composition to save resources
                context.unregisterComponentCallbacks(componentCallbacks)
                mapLifecycleController!!.onLifecycleDetached()
                composition?.deactivate()
                // Call onStop/onPause or something? Set map type to None to save resources? Because the MapView is detached.
            },
            onRelease = { mapView ->
                mapView.log("onRelease")
                context.unregisterComponentCallbacks(componentCallbacks)
                // Dispose composition
                composition?.dispose()
                // Invoke onDestroy + remove lifecycle callbacks for the MapView
                mapLifecycleController!!.onDestroy()
                // Clean up MapView.
                mapView.removeAllViews()
            },
            update = { mapView ->
                mapView.log("update")
                if (mapLifecycleController == null) {
                    debugMapReused = true
                    mapLifecycleController = MapViewLifecycleController(
                        isMapReused = true,
                        mapView = mapView
                    )
                }

                if (componentCallbacks == null) {
                    componentCallbacks = mapView.componentCallbacks()
                }

                mapLifecycleController!!.lifecycle = lifecycle

                // Create Composition
                if(!isCompositionSet) {
                    isCompositionSet = true
                    mapUpdaterScope.launch {
                        setComposition(mapView)
                        debugMapId = mapView.getTag(R.id.maps_compose_map_view_tag_debug_id) as? Int
                    }
                }
            }
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Text("Map reused: $debugMapReused")
            Text("Composition ID: $debugCompositionId")
            Text("MapView ID: $debugMapId")
        }
    }
}

internal fun MapView.log(msg: String) {
    Log.d(TAG, "[AndroidView/${this.getTag(R.id.maps_compose_map_view_tag_debug_id)}] $msg")
}

internal suspend inline fun disposingComposition(factory: () -> Composition) {
    val composition = factory()
    try {
        awaitCancellation()
    } finally {
        composition.dispose()
    }
}

private class MapViewLifecycleController(
    isMapReused: Boolean,
    private val mapView: MapView
) {
    private var previousState = if (isMapReused)
        Lifecycle.Event.ON_STOP else
        Lifecycle.Event.ON_CREATE

    var lifecycle: Lifecycle? = null
        set(value) {
            if (field !== value) {
                field?.removeObserver(observer)
                value?.addObserver(observer)
                field = value
            }
        }

    fun onDestroy() {
        lifecycle = null
        mapView.onDestroy()
    }

    fun onLifecycleDetached() {
        lifecycle = null
        mapView.onStop()
    }

    private val observer = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                // Skip calling mapView.onCreate if the lifecycle did not go through onDestroy - in
                // this case the GoogleMap composable also doesn't leave the composition. So,
                // recreating the map does not restore state properly which must be avoided.
                if (previousState != Lifecycle.Event.ON_STOP) {
                    mapView.onCreate(Bundle())
                }
            }

            Lifecycle.Event.ON_START -> mapView.onStart()
            Lifecycle.Event.ON_RESUME -> mapView.onResume()
            Lifecycle.Event.ON_PAUSE -> mapView.onPause()
            Lifecycle.Event.ON_STOP -> mapView.onStop()
            Lifecycle.Event.ON_DESTROY -> {
                // Handled in AndroidView onRelease
            }

            else -> throw IllegalStateException()
        }
        previousState = event
    }
}

private fun MapView.componentCallbacks(): ComponentCallbacks =
    object : ComponentCallbacks {
        override fun onConfigurationChanged(config: Configuration) {}

        override fun onLowMemory() {
            this@componentCallbacks.onLowMemory()
        }
    }

public typealias GoogleMapFactory = @Composable () -> Unit

/**
 * This method provides a factory pattern for GoogleMap. It can typically be used in tests to provide a default Composable
 * of type GoogleMapFactory.
 *
 * @param modifier Any modifier to be applied.
 * @param cameraPositionState The position for the map.
 * @param onMapLoaded Listener for the map loaded.
 * @param content Any content to be added.
 */
@Composable
public fun googleMapFactory(
    modifier: Modifier = Modifier,
    cameraPositionState: CameraPositionState = rememberCameraPositionState(),
    onMapLoaded: () -> Unit = {},
    content: @Composable () -> Unit = {}
): GoogleMapFactory {
    return {
        val uiSettings by remember { mutableStateOf(MapUiSettings(compassEnabled = false)) }
        val mapProperties by remember {
            mutableStateOf(MapProperties(mapType = MapType.NORMAL))
        }

        val mapVisible by remember { mutableStateOf(true) }

        if (mapVisible) {
            GoogleMap(
                modifier = modifier,
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = uiSettings,
                onMapLoaded = onMapLoaded,
                content = content
            )
        }
    }
}

