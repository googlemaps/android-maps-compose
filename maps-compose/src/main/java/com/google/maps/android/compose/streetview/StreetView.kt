package com.google.maps.android.compose.streetview

import android.content.ComponentCallbacks
import android.content.res.Configuration
import android.os.Bundle
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
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.StreetViewPanoramaOptions
import com.google.android.gms.maps.StreetViewPanoramaView
import com.google.android.gms.maps.model.StreetViewPanoramaOrientation
import com.google.maps.android.compose.MapApplier
import com.google.maps.android.compose.disposingComposition
import com.google.maps.android.ktx.awaitMap
import com.google.maps.android.ktx.awaitStreetViewPanorama
import kotlinx.coroutines.NonDisposableHandle.parent

@Composable
public fun StreetView(
    modifier: Modifier = Modifier,
    streetViewPanoramaOptionsFactory: () -> StreetViewPanoramaOptions = {
        StreetViewPanoramaOptions()
    },
    // TODO check that these defaults are correct
    isPanningGesturesEnabled: Boolean = false,
    isStreetNamesEnabled: Boolean = false,
    isUserNavigationEnabled: Boolean = false,
    isZoomGesturesEnabled: Boolean = true,
    // END TODO
    onClick: (StreetViewPanoramaOrientation) -> Unit = {},
    onLongClick: (StreetViewPanoramaOrientation) -> Unit = {},
) {
    val context = LocalContext.current
    val streetView =
        remember { StreetViewPanoramaView(context, streetViewPanoramaOptionsFactory()) }

    AndroidView(modifier = modifier, factory = { streetView }) {}
    StreetViewLifecycle(streetView)

    val clickListeners by rememberUpdatedState(StreetViewPanoramaClickListeners().also {
        it.onClick = onClick
        it.onLongClick = onLongClick
    })
    val parentComposition = rememberCompositionContext()

    LaunchedEffect(Unit) {
        disposingComposition {
            streetView.newComposition(parentComposition) {
                StreetViewUpdater(
                    isPanningGesturesEnabled = isPanningGesturesEnabled,
                    isStreetNamesEnabled = isStreetNamesEnabled,
                    isUserNavigationEnabled = isUserNavigationEnabled,
                    isZoomGesturesEnabled = isZoomGesturesEnabled,
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