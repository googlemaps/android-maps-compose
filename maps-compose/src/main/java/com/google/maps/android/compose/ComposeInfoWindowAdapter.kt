// Copyright 2022 Google LLC
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

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.runtime.Composable
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.Marker

/**
 * An InfoWindowAdapter that returns a [View] for drawing a marker's
 * info window.
 *
 * Note: As of version 18.0.2 of the Maps SDK, info windows are drawn by
 * creating a bitmap of the [View]s returned in the [GoogleMap.InfoWindowAdapter]
 * interface methods. For [getInfoContents], the Maps SDK also re-parents the
 * returned view into its own default info-window frame, so the view handed back
 * must never already have a parent.
 *
 * [ComposeView][androidx.compose.ui.platform.ComposeView] content is therefore
 * rendered into a plain [android.graphics.Bitmap] up front (while briefly attached
 * to [mapView] to drive composition), and that bitmap is wrapped in a fresh,
 * parent-less [ImageView] for the Maps SDK to use.
 */
internal class ComposeInfoWindowAdapter(
    private val mapView: MapView,
    private val markerNodeFinder: (Marker) -> MarkerNode?
) : GoogleMap.InfoWindowAdapter {

    override fun getInfoContents(marker: Marker): View? {
        val markerNode = markerNodeFinder(marker) ?: return null
        val content = markerNode.infoContent ?: return null
        return renderToImageView(markerNode) { content(marker) }
    }

    override fun getInfoWindow(marker: Marker): View? {
        val markerNode = markerNodeFinder(marker) ?: return null
        val infoWindow = markerNode.infoWindow ?: return null
        return renderToImageView(markerNode) { infoWindow(marker) }
    }

    private fun renderToImageView(
        markerNode: MarkerNode,
        content: @Composable () -> Unit,
    ): View? {
        val bitmap = mapView.renderComposableToBitmap(markerNode.compositionContext, content)
            ?: return null
        return ImageView(mapView.context).apply {
            layoutParams = ViewGroup.LayoutParams(bitmap.width, bitmap.height)
            scaleType = ImageView.ScaleType.FIT_XY
            setImageBitmap(bitmap)
        }
    }

}
