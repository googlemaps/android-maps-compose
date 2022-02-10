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

    private val infoWindowView by lazy {
        val window = ComposeView(mapView.context)
        mapView.addView(window)
        window
    }

    override fun getInfoContents(marker: Marker): View? =
        marker.infoWindowView<ComposeInfoWindowContent>()

    override fun getInfoWindow(marker: Marker): View? =
        marker.infoWindowView<ComposeInfoWindow>()

    private inline fun <reified T : ComposeInfoWindowComponent> Marker.infoWindowView(): View? {
        val markerNode = markerNodeFinder(this) ?: return null
        val component = markerNode.infoWindowComponent as? T ?: return null
        val infoWindowView = infoWindowView.apply {
            setParentCompositionContext(markerNode.compositionContext)
            setContent {
                component.content(this@infoWindowView)
            }
        }
        (infoWindowView.parent as? MapView)?.removeView(infoWindowView)
        return infoWindowView
    }
}