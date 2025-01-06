// Copyright 2024 Google LLC
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
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.LocationSource
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapColorScheme
import com.google.android.gms.maps.model.PointOfInterest
import com.google.maps.android.ktx.awaitMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch

/**
 * A compose container for a [MapView].
 *
 * @param modifier Modifier to be applied to the GoogleMap
 * @param mergeDescendants deactivates the map for accessibility purposes
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
 * @param mapColorScheme Defines the color scheme for the Map.
 * @param content the content of the map
 */
@Composable
public fun GoogleMap(
    modifier: Modifier = Modifier,
    mergeDescendants: Boolean = false,
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
    contentPadding: PaddingValues = DefaultMapContentPadding,
    mapColorScheme: ComposeMapColorScheme? = null,
    mapViewCreator: ((Context, GoogleMapOptions) -> MapView)? = null,
    content: @Composable @GoogleMapComposable () -> Unit = {},
) {
    // When in preview, early return a Box with the received modifier preserving layout
    if (LocalInspectionMode.current) {
        Box(modifier = modifier)
        return
    }

    // rememberUpdatedState and friends are used here to make these values observable to
    // the subcomposition without providing a new content function each recomposition
    val mapClickListeners = remember { MapClickListeners() }.also {
        it.indoorStateChangeListener = indoorStateChangeListener
        it.onMapClick = onMapClick
        it.onMapLongClick = onMapLongClick
        it.onMapLoaded = onMapLoaded
        it.onMyLocationButtonClick = onMyLocationButtonClick
        it.onMyLocationClick = onMyLocationClick
        it.onPOIClick = onPOIClick
    }

    val mapUpdaterState = remember {
        MapUpdaterState(
            mergeDescendants,
            contentDescription,
            cameraPositionState,
            contentPadding,
            locationSource,
            properties,
            uiSettings,
            mapColorScheme?.value,
        )
    }.also {
        it.mergeDescendants = mergeDescendants
        it.contentDescription = contentDescription
        it.cameraPositionState = cameraPositionState
        it.contentPadding = contentPadding
        it.locationSource = locationSource
        it.mapProperties = properties
        it.mapUiSettings = uiSettings
        it.mapColorScheme = mapColorScheme?.value
    }

    val parentComposition = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    var subcompositionJob by remember { mutableStateOf<Job?>(null) }
    val parentCompositionScope = rememberCoroutineScope()

    AndroidView(
        modifier = modifier,
        factory = { context ->
            if (mapViewCreator != null) {
                mapViewCreator(context, googleMapOptionsFactory())
            } else {
                MapView(context, googleMapOptionsFactory())
            }.also { mapView ->
                val componentCallbacks = object : ComponentCallbacks2 {
                    override fun onConfigurationChanged(newConfig: Configuration) {}
                    @Deprecated("Deprecated in Java", ReplaceWith("onTrimMemory(level)"))
                    override fun onLowMemory() { mapView.onLowMemory() }
                    override fun onTrimMemory(level: Int) { mapView.onLowMemory() }
                }
                context.registerComponentCallbacks(componentCallbacks)

                val lifecycleObserver = MapLifecycleEventObserver(mapView)

                mapView.tag = MapTagData(componentCallbacks, lifecycleObserver)

                // Only register for [lifecycleOwner]'s lifecycle events while MapView is attached
                val onAttachStateListener = object : View.OnAttachStateChangeListener {
                    private var lifecycle: Lifecycle? = null

                    override fun onViewAttachedToWindow(mapView: View) {
                        lifecycle = mapView.findViewTreeLifecycleOwner()!!.lifecycle.also {
                            it.addObserver(lifecycleObserver)
                        }
                    }

                    override fun onViewDetachedFromWindow(v: View) {
                        lifecycle?.removeObserver(lifecycleObserver)
                        lifecycle = null
                        lifecycleObserver.moveToBaseState()
                    }
                }

                mapView.addOnAttachStateChangeListener(onAttachStateListener)
            }
        },
        onReset = { /* View is detached. */ },
        onRelease = { mapView ->
            val (componentCallbacks, lifecycleObserver) = mapView.tagData
            mapView.context.unregisterComponentCallbacks(componentCallbacks)
            lifecycleObserver.moveToDestroyedState()
            mapView.tag = null
        },
        update = { mapView ->
            if (subcompositionJob == null) {
                subcompositionJob = parentCompositionScope.launchSubcomposition(
                    mapUpdaterState,
                    parentComposition,
                    mapView,
                    mapClickListeners,
                    currentContent,
                )
            }
        }
    )
}

/**
 * Create and apply the [content] compositions to the map +
 * dispose the [Composition] when the parent composable is disposed.
 * */
private fun CoroutineScope.launchSubcomposition(
    mapUpdaterState: MapUpdaterState,
    parentComposition: CompositionContext,
    mapView: MapView,
    mapClickListeners: MapClickListeners,
    content: @Composable @GoogleMapComposable () -> Unit,
): Job {
    // Use [CoroutineStart.UNDISPATCHED] to kick off GoogleMap loading immediately
    return launch(start = CoroutineStart.UNDISPATCHED) {
        val map = mapView.awaitMap()
        val composition = Composition(
            applier = MapApplier(map, mapView, mapClickListeners),
            parent = parentComposition
        )

        try {
            composition.setContent {
                MapUpdater(mapUpdaterState)

                MapClickListenerUpdater()

                CompositionLocalProvider(
                    LocalCameraPositionState provides mapUpdaterState.cameraPositionState,
                    content
                )
            }
            awaitCancellation()
        } finally {
            composition.dispose()
        }
    }
}

@Stable
internal class MapUpdaterState(
    mergeDescendants: Boolean,
    contentDescription: String?,
    cameraPositionState: CameraPositionState,
    contentPadding: PaddingValues,
    locationSource: LocationSource?,
    mapProperties: MapProperties,
    mapUiSettings: MapUiSettings,
    mapColorScheme: Int?,
) {
    var mergeDescendants by mutableStateOf(mergeDescendants)
    var contentDescription by mutableStateOf(contentDescription)
    var cameraPositionState by mutableStateOf(cameraPositionState)
    var contentPadding by mutableStateOf(contentPadding)
    var locationSource by mutableStateOf(locationSource)
    var mapProperties by mutableStateOf(mapProperties)
    var mapUiSettings by mutableStateOf(mapUiSettings)
    var mapColorScheme by mutableStateOf(mapColorScheme)
}

/** Used to store things in the tag which must be retrievable across recompositions */
private data class MapTagData(
    val componentCallbacks: ComponentCallbacks,
    val lifecycleObserver: MapLifecycleEventObserver
)

private val MapView.tagData: MapTagData
    get() = tag as MapTagData

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

private class MapLifecycleEventObserver(private val mapView: MapView) : LifecycleEventObserver {
    private var currentLifecycleState: Lifecycle.State = Lifecycle.State.INITIALIZED

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            // [mapView.onDestroy] is only invoked from AndroidView->onRelease.
            Lifecycle.Event.ON_DESTROY -> moveToBaseState()
            else -> moveToLifecycleState(event.targetState)
        }
    }

    /**
     * Move down to [Lifecycle.State.CREATED] but only if [currentLifecycleState] is actually above that.
     * It's theoretically possible that [currentLifecycleState] is still in [Lifecycle.State.INITIALIZED] state.
     * */
    fun moveToBaseState() {
        if (currentLifecycleState > Lifecycle.State.CREATED) {
            moveToLifecycleState(Lifecycle.State.CREATED)
        }
    }

    fun moveToDestroyedState() {
        if (currentLifecycleState > Lifecycle.State.INITIALIZED) {
            moveToLifecycleState(Lifecycle.State.DESTROYED)
        }
    }

    private fun moveToLifecycleState(targetState: Lifecycle.State) {
        while (currentLifecycleState != targetState) {
            when {
                currentLifecycleState < targetState -> moveUp()
                currentLifecycleState > targetState -> moveDown()
            }
        }
    }

    private fun moveDown() {
        val event = Lifecycle.Event.downFrom(currentLifecycleState)
            ?: error("no event down from $currentLifecycleState")
        invokeEvent(event)
    }

    private fun moveUp() {
        val event = Lifecycle.Event.upFrom(currentLifecycleState)
            ?: error("no event up from $currentLifecycleState")
        invokeEvent(event)
    }

    private fun invokeEvent(event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
            Lifecycle.Event.ON_START -> mapView.onStart()
            Lifecycle.Event.ON_RESUME -> mapView.onResume()
            Lifecycle.Event.ON_PAUSE -> mapView.onPause()
            Lifecycle.Event.ON_STOP -> mapView.onStop()
            Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
            else -> error("Unsupported lifecycle event: $event")
        }
        currentLifecycleState = event.targetState
    }
}

/**
 * Enum representing a 1-1 mapping to [com.google.android.gms.maps.model.MapColorScheme].
 *
 * This enum provides equivalent values to facilitate usage with [com.google.maps.android.compose.GoogleMap].
 *
 * @param value The integer value corresponding to each map color scheme.
 */
public enum class ComposeMapColorScheme(public val value: Int) {
    LIGHT(MapColorScheme.LIGHT),
    DARK(MapColorScheme.DARK),
    FOLLOW_SYSTEM(MapColorScheme.FOLLOW_SYSTEM);
}
