package com.google.maps.android.compose.navigation

import android.content.ComponentCallbacks2
import android.os.Bundle
import com.google.android.gms.maps.GoogleMap
import com.google.android.libraries.navigation.NavigationView
import com.google.maps.android.compose.AbstractMapViewDelegate
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class NavigationViewDelegate(override val mapView: NavigationView) : AbstractMapViewDelegate<NavigationView> {
    override fun onCreate(savedInstanceState: Bundle?): Unit = mapView.onCreate(savedInstanceState)
    override fun onStart(): Unit = mapView.onStart()
    override fun onResume(): Unit = mapView.onResume()
    override fun onPause(): Unit = mapView.onPause()
    override fun onStop(): Unit = mapView.onStop()

    override fun onLowMemory() {
        mapView.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
    }

    override fun onDestroy(): Unit = mapView.onDestroy()

    override suspend fun awaitMap(): GoogleMap = mapView.awaitMap()
}

suspend inline fun NavigationView.awaitMap(): GoogleMap = suspendCoroutine { continuation ->
    getMapAsync {
        continuation.resume(it)
    }
}
