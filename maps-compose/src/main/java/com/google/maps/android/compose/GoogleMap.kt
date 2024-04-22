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

@file:Suppress("RemoveRedundantQualifierName")

package com.google.maps.android.compose

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.LocationSource
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.google.maps.android.ktx.awaitMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch

internal const val TAG = "GoogleMap"

private var compositionCounter = 0

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
    val mapClickListeners = remember { MapClickListeners() }.also {
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

    val context = LocalContext.current

    // Debug stuff
    val debugCompositionId = remember { compositionCounter++ }
    var debugMapId: Int? by remember { mutableStateOf(null) }
    var debugIsMapReused: Boolean? by remember { mutableStateOf(null) }

    /**
     * Create and apply the [content] compositions to the map +
     * dispose the [Composition] when the parent composable is disposed.
     * */
    fun CoroutineScope.launchComposition(mapView: MapView): Job {
        mapView.log("Creating composition...")

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

        return launch(start = CoroutineStart.UNDISPATCHED) {
            val composition = mapView.createComposition(mapClickListeners, parentComposition).apply {
                setContent(mapCompositionContent)
            }

            try {
                awaitCancellation()
            } finally {
                mapView.log("Disposing composition...")
                composition.dispose()
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    var isCompositionSet by remember { mutableStateOf(false) }
    val mapUpdaterScope = rememberCoroutineScope()

    AndroidView(
        modifier = modifier,
        factory = {
            debugIsMapReused = false
            MapView(context, googleMapOptionsFactory()).also { mapView ->
                mapView.log("Creating MapView")
                mapView.registerAndSaveNewComponentCallbacks(context)

                // Used to observe lifecycle owner's lifecycle state and
                // to gradually move between states.
                mapView.tagData().lifecycleRegistry = MapViewDerivedLifecycleRegistry(lifecycleOwner, mapView)
            }
        },
        onReset = { mapView ->
            mapView.log("ON RESET!")
            // View is detached.
            mapView.tagData().lifecycleRegistry!!.overrideLifecycleState(Lifecycle.State.CREATED)
        },
        onRelease = { mapView ->
            mapView.log("onRelease")

            mapView.tagData().let { tagData ->
                tagData.componentCallbacks?.let { componentCallbacks ->
                    tagData.mapViewContext?.unregisterComponentCallbacks(componentCallbacks)
                }

                tagData.lifecycleRegistry!!.destroy()
            }

            mapView.tag = null
        },
        update = { mapView ->
            // Create Composition
            if (!isCompositionSet) {
                isCompositionSet = true
                debugMapId = mapView.tagData().debugId
                mapUpdaterScope.launchComposition(mapView)
            }

            mapView.tagData().lifecycleRegistry!!.run {
                tryUpdateLifecycleOwner(mapView.findViewTreeLifecycleOwner()!!)
                clearOverwrittenLifecycleState()
            }
        }
    )

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Text("Map reused: $debugIsMapReused")
            Text("Composition ID: $debugCompositionId")
            Text("MapView ID: $debugMapId")
        }
    }
}

// TODO is there a better name?
/**
 * LifecycleRegistry which observes [lifecycleOwner] and dispatches incoming events.
 * Also supports overriding current state.
 * */
internal abstract class DerivedLifecycleRegistry(
    lifecycleOwner: LifecycleOwner
): LifecycleRegistry(lifecycleOwner) {
    private var lifecycleOwnerState = Lifecycle.State.INITIALIZED
    private var overwrittenLifecycleState: Lifecycle.State? = null

    private var currentLifecycleOwner = lifecycleOwner

    private val lifecycleOwnerObserver = LifecycleEventObserver { _, event ->
        if (event == Event.ON_DESTROY) {
            // Parent lifecycle reached DESTROYED state. Unregister LifecycleObserver.
            removeCurrentLifecycleOwnerObserver()
        }
        if (overwrittenLifecycleState == null) {
            handleLifecycleEvent(event)
        }
        lifecycleOwnerState = event.targetState
    }

    private val lifecycleEventObserver = object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Event) {
            if (event == Event.ON_DESTROY) {
                // This listener has received Event.ON_DESTROY. Won't receive more lifecycle events.
                this@DerivedLifecycleRegistry.removeObserver(this)
            }
            onLifecycleEvent(event)
        }
    }

    private fun removeCurrentLifecycleOwnerObserver() {
        currentLifecycleOwner.lifecycle.removeObserver(lifecycleOwnerObserver)
    }

    @Synchronized
    fun tryUpdateLifecycleOwner(lifecycleOwner: LifecycleOwner) {
        if(lifecycleOwner == currentLifecycleOwner) return

        // There is a new LifecycleOwner. Let's change it.
        currentLifecycleOwner.lifecycle.removeObserver(lifecycleOwnerObserver)
        currentLifecycleOwner = lifecycleOwner
        lifecycleOwner.lifecycle.addObserver(lifecycleOwnerObserver)
    }

    abstract fun onLifecycleEvent(event: Lifecycle.Event)

    fun initObserver() {
        addObserver(lifecycleEventObserver)
        currentLifecycleOwner.lifecycle.addObserver(lifecycleOwnerObserver)
    }

    fun overrideLifecycleState(state: Lifecycle.State) {
        overwrittenLifecycleState = state
        currentState = state
    }

    fun clearOverwrittenLifecycleState() {
        overwrittenLifecycleState ?: return
        overwrittenLifecycleState = null
        currentState = lifecycleOwnerState
    }

    fun destroy() {
        currentLifecycleOwner.lifecycle.removeObserver(lifecycleOwnerObserver)
        currentState = Lifecycle.State.DESTROYED
        removeObserver(lifecycleEventObserver)
    }
}

internal class MapViewDerivedLifecycleRegistry(
    lifecycleOwner: LifecycleOwner,
    private val mapView: MapView
) : DerivedLifecycleRegistry(lifecycleOwner) {

    init {
        super.initObserver()
    }

    override fun onLifecycleEvent(event: Lifecycle.Event) {
        mapView.log("Invoking $event")
        when (event) {
            Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
            Lifecycle.Event.ON_START -> mapView.onStart()
            Lifecycle.Event.ON_RESUME -> mapView.onResume()
            Lifecycle.Event.ON_PAUSE -> mapView.onPause()
            Lifecycle.Event.ON_STOP -> mapView.onStop()
            Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
            else -> error("Unsupported Lifecycle.Event: $event")
        }
    }
}

private fun MapView.registerAndSaveNewComponentCallbacks(context: Context) {
    val newComponentCallbacks = this.componentCallbacks()
    val tagData = tagData()
    tagData.componentCallbacks = newComponentCallbacks
    tagData.mapViewContext = context
    context.registerComponentCallbacks(newComponentCallbacks)
}

internal data class MapTagData(
    var componentCallbacks: ComponentCallbacks?,
    var mapViewContext: Context?,
    var lifecycleRegistry: DerivedLifecycleRegistry?,
    val debugId: Int = nextId
) {
    companion object {
        private var nextId = 0
            get() = field++
    }
}

// TODO make private
internal fun MapView.tagData(): MapTagData = tag as? MapTagData ?: run {
    MapTagData(null, null, null).also { newTag ->
        tag = newTag
    }
}

private suspend fun MapView.createComposition(
    mapClickListeners: MapClickListeners,
    parentComposition: CompositionContext
): Composition {
    val map = awaitMap()
    return Composition(
        applier = MapApplier(map, this, mapClickListeners),
        parent = parentComposition
    )
}

internal fun MapView.log(msg: String, tag: String = TAG) {
    Log.d(tag, "[MapView/${tagData().debugId}] $msg")
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

