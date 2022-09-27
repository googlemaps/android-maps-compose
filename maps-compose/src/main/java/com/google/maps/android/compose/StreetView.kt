package com.google.maps.android.compose

import android.content.ComponentCallbacks
import android.content.res.Configuration
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.StreetViewPanoramaView
import com.google.android.gms.maps.model.StreetViewPanoramaLocation
import com.google.maps.android.ktx.awaitStreetViewPanorama

@Composable
public fun StreetView(
    modifier: Modifier = Modifier,
    //isZoomGesturesEnabled: Boolean = true
) {
    val context = LocalContext.current
    val streetView = remember { StreetViewPanoramaView(context) }
    AndroidView(modifier = modifier, factory = { streetView }) {}
    StreetViewLifecycle(streetView)
    LaunchedEffect(Unit) {
        val streetViewPanorama = streetView.awaitStreetViewPanorama()
        //streetViewPanorama.setPosition
        //streetViewPanorama.location = StreetViewPanoramaLocation()
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