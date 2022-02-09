package com.google.maps.android.compose

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.platform.ComposeView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.Marker

/**
 * An InfoWindowAdapter that returns a [ComposeView] for drawing an marker's
 * info window.
 *
 * Note: As of version 18.0.2 of the Maps SDK, info windows are drawn by
 * creating a bitmap of the [View]s return in the [GoogleMap.InfoWindowAdapter]
 * interface methods. The returned views are never attached to a window,
 * instead, they are drawn to a bitmap canvas. This breaks the assumption
 * [ComposeView] makes where it must eventually be attached to a window. As a
 * workaround, the contained window is temporarily attached to the MapView so
 * that the contents of the ComposeViews are rendered.
 *
 * Eventually when info windows are no longer implemented this way, this
 * implementation should be updated.
 */
internal class ComposeInfoWindowAdapter(
    private val mapView: MapView,
    private val markerNodeFinder: (Marker) -> MarkerNode?
) : GoogleMap.InfoWindowAdapter {

    private val infoWindowContents by lazy {
        val contents = ComposeView(mapView.context)
        mapView.addView(contents)
        contents
    }
    private val infoWindow by lazy {
        val window = ComposeView(mapView.context)
        mapView.addView(window)
        window
    }

    override fun getInfoContents(marker: Marker): View? {
        val markerNode = markerNodeFinder(marker)
        val content = markerNode?.infoWindowContent
        if (content == null) {
            // No custom contents, use default
            return null
        }

        return infoWindowContents.applyAndRemove(markerNode.compositionContext) {
            content(marker)
        }
    }

    override fun getInfoWindow(marker: Marker): View? {
        val markerNode = markerNodeFinder(marker)
        val window = markerNode?.infoWindow

        if (window == null) {
            // No custom window, use default
            return null
        }

        return infoWindow.applyAndRemove(markerNode.compositionContext) {
            window(marker)
        }
    }

    private fun ComposeView.applyAndRemove(
        parentContext: CompositionContext,
        content: @Composable () -> Unit
    ): ComposeView {
        val result = this.apply {
            setParentCompositionContext(parentContext)
            setContent(content)
        }
        (this.parent as? MapView)?.removeView(this)
        return result
    }
}