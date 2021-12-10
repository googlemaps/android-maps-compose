// Copyright 2021 Google LLC
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

import android.graphics.PointF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.Stable
import androidx.compose.runtime.currentComposer
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.ktx.addMarker

@Stable
internal data class MarkerNode(
    val marker: Marker,
    var onMarkerClick: (Marker) -> Boolean
)

/**
 * A composable for a marker on the map.
 *
 * @param position the position of the marker
 * @param alpha the alpha (opacity) of the marker
 * @param anchor the anchor for the marker image
 * @param draggable sets the draggability for the marker
 * @param flat sets if the marker should be flat against the map
 * @param icon sets the icon for the marker
 * @param infoWindowAnchor the anchor point of the info window on the marker image
 * @param rotation the rotation of the marker in degrees clockwise about the marker's anchor point
 * @param snippet the snippet for the marker
 * @param title the title for the marker
 * @param visible the visibility of the marker
 * @param zIndex the z-index of the marker
 * @param onClick the z-index of the marker
 */
@Composable
fun GoogleMapScope.Marker(
    position: LatLng,
    alpha: Float = 1.0f,
    anchor: PointF = PointF(0.5f, 1.0f),
    draggable: Boolean = false,
    flat: Boolean = false,
    icon: BitmapDescriptor? = null,
    infoWindowAnchor: PointF = PointF(0.5f, 0.0f),
    rotation: Float = 0.0f,
    snippet: String? = null,
    title: String? = null,
    visible: Boolean = true,
    zIndex: Float = 0.0f,
    onClick: (Marker) -> Boolean = { false }
) {
    if (currentComposer.applier !is MapApplier) error("Invalid Applier.")
    val mapApplier = currentComposer.applier as MapApplier
    ComposeNode<MarkerNode, MapApplier>(
        factory = {
            val marker = mapApplier.map.addMarker {
                alpha(alpha)
                anchor(anchor.x, anchor.y)
                draggable(draggable)
                flat(flat)
                icon(icon)
                infoWindowAnchor(infoWindowAnchor.x, infoWindowAnchor.y)
                position(position)
                rotation(rotation)
                snippet(snippet)
                title(title)
                visible(visible)
                zIndex(zIndex)
            } ?: error("Error adding marker")
            MarkerNode(marker, onClick)
        },
        update = {
            set(onClick) { this.onMarkerClick = it}

            set(alpha) { this.marker.alpha = it }
            set(anchor) { this.marker.setAnchor(it.x, it.y) }
            set(draggable) { this.marker.isDraggable = it }
            set(flat) { this.marker.isFlat = it }
            set(icon) { this.marker.setIcon(it) }
            set(infoWindowAnchor) { this.marker.setInfoWindowAnchor(it.x, it.y) }
            set(position) { this.marker.position = it }
            set(rotation) { this.marker.rotation = it }
            set(snippet) { this.marker.snippet = it }
            set(title) { this.marker.title = it}
            set(visible) { this.marker.isVisible = it}
            set(zIndex) { this.marker.zIndex = it}
        }
    )
}