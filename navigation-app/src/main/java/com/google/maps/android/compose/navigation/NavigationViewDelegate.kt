package com.google.maps.android.compose.navigation

import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.platform.AbstractComposeView
import com.google.android.gms.maps.GoogleMap
import com.google.android.libraries.navigation.NavigationView
import com.google.maps.android.compose.AbstractMapViewDelegate
import com.google.maps.android.compose.ComposeUiViewRenderer
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class NavigationViewDelegate(override val mapView: NavigationView) :
    AbstractMapViewDelegate<NavigationView> {
    override fun onCreate(savedInstanceState: Bundle?): Unit = mapView.onCreate(savedInstanceState)
    override fun onStart(): Unit = mapView.onStart()
    override fun onResume(): Unit = mapView.onResume()
    override fun onPause(): Unit = mapView.onPause()
    override fun onStop(): Unit = mapView.onStop()

    override fun onLowMemory() {
        mapView.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
    }

    override fun onDestroy(): Unit = mapView.onDestroy()

    override suspend fun awaitMap(): GoogleMap = mapView.awaitMap()

    override fun renderComposeViewOnce(
        view: AbstractComposeView,
        parentContext: CompositionContext,
        onAddedToWindow: ((View) -> Unit)?
    ) {
        mapView.renderComposeViewOnce(
            view = view,
            parentContext = parentContext,
            onAddedToWindow = onAddedToWindow
        )
    }

    override fun startRenderingComposeView(
        view: AbstractComposeView,
        parentContext: CompositionContext
    ): ComposeUiViewRenderer.RenderHandle {
        return mapView.startRenderingComposeView(
            view = view,
            parentContext = parentContext,
        )
    }
}

suspend inline fun NavigationView.awaitMap(): GoogleMap =
    suspendCoroutine { continuation ->
        getMapAsync {
            continuation.resume(it)
        }
    }

private fun NavigationView.renderComposeViewOnce(
    view: AbstractComposeView,
    onAddedToWindow: ((View) -> Unit)? = null,
    parentContext: CompositionContext,
) {
    startRenderingComposeView(view, parentContext).use {
        onAddedToWindow?.invoke(view)
    }
}

private fun NavigationView.startRenderingComposeView(
    view: AbstractComposeView,
    parentContext: CompositionContext,
): ComposeUiViewRenderer.RenderHandle {
    val containerView = ensureContainerView()
    containerView.addView(view)
    view.apply {
        setParentCompositionContext(parentContext)
    }
    return object : ComposeUiViewRenderer.RenderHandle {
        override fun dispose() {
            containerView.removeView(view)
        }

    }
}

private fun NavigationView.ensureContainerView(): com.google.maps.android.compose.navigation.NoDrawContainerView {
    return findViewById(R.id.maps_compose_nodraw_container_view)
        ?: NoDrawContainerView(context)
            .apply { id = R.id.maps_compose_nodraw_container_view }
            .also(::addView)
}

private class NoDrawContainerView(context: Context) : ViewGroup(context) {
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    }

    override fun dispatchDraw(canvas: Canvas) {
    }
}
