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
import android.view.View
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
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

    var isCompositionSet by remember { mutableStateOf(false) }
    val mapUpdaterScope = rememberCoroutineScope()

    AndroidView(
        modifier = modifier,
        factory = {
            debugIsMapReused = false
            MapView(context, googleMapOptionsFactory()).also { mapView ->
                mapView.log("Creating MapView")
                mapView.registerAndSaveNewComponentCallbacks(context)

                fun log(msg: String) = mapView.log(msg)

                val lifecycleObserver = MapLifecycleEventObserver(mapView)

                var lifecycleOwner: LifecycleOwner? = null

                fun unregisterLifecycleObserver() {
                    log("Unregistering lifecycle observer")
                    lifecycleOwner?.lifecycle?.removeObserver(lifecycleObserver)
                    lifecycleOwner = null
                }

                val onAttachStateListener = object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(mapView: View) {
                        log("View attached!")
                        lifecycleOwner = mapView.findViewTreeLifecycleOwner()!!.also {
                            it.lifecycle.addObserver(lifecycleObserver)
                        }
                    }

                    override fun onViewDetachedFromWindow(v: View) {
                        log("View detached!")
                        unregisterLifecycleObserver()
                        lifecycleObserver.moveToBaseState()
                    }
                }

                mapView.addOnAttachStateChangeListener(onAttachStateListener)

                mapView.tagData().onRelease = {
                    unregisterLifecycleObserver()
                    mapView.removeOnAttachStateChangeListener(onAttachStateListener)
                    lifecycleObserver.moveToLifecycleState(Lifecycle.State.DESTROYED)
                }
            }
        },
        onReset = { /* View is detached. */ },
        onRelease = { mapView ->
            mapView.log("onRelease")

            mapView.tagData().let { tagData ->
                tagData.componentCallbacks?.let { componentCallbacks ->
                    tagData.mapViewContext?.unregisterComponentCallbacks(componentCallbacks)
                }

                tagData.onRelease!!.invoke()
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
    var onRelease: (() -> Unit)?,
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
        if(currentLifecycleState > Lifecycle.State.CREATED) {
            moveToLifecycleState(Lifecycle.State.CREATED)
        }
    }

    @Synchronized
    fun moveToLifecycleState(targetState: Lifecycle.State) {
        while(currentLifecycleState != targetState) {
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
        mapView.log("Invoking Lifecycle event $event")
        when(event) {
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