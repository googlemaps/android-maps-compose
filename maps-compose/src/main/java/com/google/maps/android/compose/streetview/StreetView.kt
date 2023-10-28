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

package com.google.maps.android.compose.streetview

import android.content.ComponentCallbacks
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.StreetViewPanoramaOptions
import com.google.android.gms.maps.StreetViewPanoramaView
import com.google.android.gms.maps.model.StreetViewPanoramaOrientation
import com.google.maps.android.compose.disposingComposition
import com.google.maps.android.ktx.MapsExperimentalFeature
import com.google.maps.android.ktx.awaitStreetViewPanorama

/**
 * A composable for displaying a Street View for a given location. A location might not be available for a given
 * set of coordinates. We recommend you to check our sample on [StreetViewActivity] using our utility function
 * in [StreetViewUtils] to manage non-existing locations.
 *
 *
 *
 * @param modifier Modifier to be applied to the StreetView
 * @param cameraPositionState the [StreetViewCameraPositionState] to be used to control or observe
 * the Street View's camera
 * @param streetViewPanoramaOptionsFactory a factory lambda for providing a
 * [StreetViewPanoramaOptions] object which is used when the underlying [StreetViewPanoramaView] is
 * constructed
 * @param isPanningGesturesEnabled whether panning gestures are enabled or not
 * @param isStreetNamesEnabled whether street names are enabled or not
 * @param isUserNavigationEnabled whether user navigation is enabled or not
 * @param isZoomGesturesEnabled whether zoom gestures are enabled or not
 * @param onClick lambda to receive events when the Street View is clicked
 * @param onLongClick lambda to receive events when the Street View is long clicked
 */
@MapsExperimentalFeature
@Composable
public fun StreetView(
    modifier: Modifier = Modifier,
    cameraPositionState: StreetViewCameraPositionState = rememberStreetViewCameraPositionState(),
    streetViewPanoramaOptionsFactory: () -> StreetViewPanoramaOptions = {
        StreetViewPanoramaOptions()
    },
    isPanningGesturesEnabled: Boolean = true,
    isStreetNamesEnabled: Boolean = true,
    isUserNavigationEnabled: Boolean = true,
    isZoomGesturesEnabled: Boolean = true,
    onClick: (StreetViewPanoramaOrientation) -> Unit = {},
    onLongClick: (StreetViewPanoramaOrientation) -> Unit = {},
) {
    val context = LocalContext.current
    val streetView =
        remember(context) { StreetViewPanoramaView(context, streetViewPanoramaOptionsFactory()) }

    AndroidView(modifier = modifier, factory = { streetView }) {}
    StreetViewLifecycle(streetView)

    val currentCameraPositionState by rememberUpdatedState(cameraPositionState)
    val currentIsPanningGestureEnabled by rememberUpdatedState(isPanningGesturesEnabled)
    val currentIsStreetNamesEnabled by rememberUpdatedState(isStreetNamesEnabled)
    val currentIsUserNavigationEnabled by rememberUpdatedState(isUserNavigationEnabled)
    val currentIsZoomGesturesEnabled by rememberUpdatedState(isZoomGesturesEnabled)
    val clickListeners by rememberUpdatedState(StreetViewPanoramaEventListeners().also {
        it.onClick = onClick
        it.onLongClick = onLongClick
    })
    val parentComposition = rememberCompositionContext()

    LaunchedEffect(Unit) {
        disposingComposition {
            streetView.newComposition(parentComposition) {
                StreetViewUpdater(
                    cameraPositionState = currentCameraPositionState,
                    isPanningGesturesEnabled = currentIsPanningGestureEnabled,
                    isStreetNamesEnabled = currentIsStreetNamesEnabled,
                    isUserNavigationEnabled = currentIsUserNavigationEnabled,
                    isZoomGesturesEnabled = currentIsZoomGesturesEnabled,
                    clickListeners = clickListeners
                )
            }
        }
    }
}

@Composable
private fun StreetViewLifecycle(streetView: StreetViewPanoramaView) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val previousState = remember { mutableStateOf(Lifecycle.Event.ON_CREATE) }
    DisposableEffect(context, lifecycle, streetView) {
        val streetViewLifecycleObserver = streetView.lifecycleObserver(previousState)
        val callbacks = streetView.componentCallbacks()

        lifecycle.addObserver(streetViewLifecycleObserver)
        context.registerComponentCallbacks(callbacks)

        onDispose {
            lifecycle.removeObserver(streetViewLifecycleObserver)
            context.unregisterComponentCallbacks(callbacks)
            streetView.onDestroy()
        }
    }
}

private suspend inline fun StreetViewPanoramaView.newComposition(
    parent: CompositionContext,
    noinline content: @Composable () -> Unit
): Composition {
    val panorama = awaitStreetViewPanorama()
    Log.d("StreetView", "Location is ${panorama.location}")
    return Composition(
        StreetViewPanoramaApplier(panorama), parent
    ).apply {
        setContent(content)
    }
}

private fun StreetViewPanoramaView.lifecycleObserver(previousState: MutableState<Lifecycle.Event>): LifecycleEventObserver =
    LifecycleEventObserver { _, event ->
        event.targetState
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                // Skip calling mapView.onCreate if the lifecycle did not go through onDestroy - in
                // this case the GoogleMap composable also doesn't leave the composition. So,
                // recreating the map does not restore state properly which must be avoided.
                if (previousState.value != Lifecycle.Event.ON_STOP) {
                    this.onCreate(Bundle())
                }
            }
            Lifecycle.Event.ON_START -> this.onStart()
            Lifecycle.Event.ON_RESUME -> this.onResume()
            Lifecycle.Event.ON_PAUSE -> this.onPause()
            Lifecycle.Event.ON_STOP -> this.onStop()
            Lifecycle.Event.ON_DESTROY -> {
                //handled in onDispose
            }
            else -> throw IllegalStateException()
        }
        previousState.value = event
    }

private fun StreetViewPanoramaView.componentCallbacks(): ComponentCallbacks =
    object : ComponentCallbacks {
        override fun onConfigurationChanged(config: Configuration) {}

        override fun onLowMemory() {
            this@componentCallbacks.onLowMemory()
        }
    }