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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.ktx.addMarker

internal class MarkerNode(
    val marker: Marker,
    var markerDragState: MarkerDragState?,
    var onMarkerClick: (Marker) -> Boolean,
    var onInfoWindowClick: (Marker) -> Unit,
    var onInfoWindowClose: (Marker) -> Unit,
    var onInfoWindowLongClick: (Marker) -> Unit,
) : MapNode {
    override fun onRemoved() {
        marker.remove()
    }
}

@Immutable
enum class DragState {
    START, DRAG, END
}

/**
 * A state object for observing marker drag events.
 */
class MarkerDragState {
    /**
     * State of the marker drag.
     */
    var dragState: DragState by mutableStateOf(DragState.END)
        internal set
}

/**
 * Creates and [remember] a [MarkerDragState].
 */
@Composable
fun rememberMarkerDragState(): MarkerDragState = remember {
    MarkerDragState()
}

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
 * @param tag optional tag to associate with the marker
 * @param title the title for the marker
 * @param visible the visibility of the marker
 * @param zIndex the z-index of the marker
 * @param markerDragState a [MarkerDragState] to be used for observing marker drag events
 * @param onClick a lambda invoked when the marker is clicked
 * @param onInfoWindowClick a lambda invoked when the marker's info window is clicked
 * @param onInfoWindowClose a lambda invoked when the marker's info window is closed
 * @param onInfoWindowLongClick a lambda invoked when the marker's info window is long clicked
 */
@Composable
fun Marker(
    position: LatLng,
    alpha: Float = 1.0f,
    anchor: Offset = Offset(0.5f, 1.0f),
    draggable: Boolean = false,
    flat: Boolean = false,
    icon: BitmapDescriptor? = null,
    infoWindowAnchor: Offset = Offset(0.5f, 0.0f),
    rotation: Float = 0.0f,
    snippet: String? = null,
    tag: Any? = null,
    title: String? = null,
    visible: Boolean = true,
    zIndex: Float = 0.0f,
    markerDragState: MarkerDragState? = null,
    onClick: (Marker) -> Boolean = { false },
    onInfoWindowClick: (Marker) -> Unit = {},
    onInfoWindowClose: (Marker) -> Unit = {},
    onInfoWindowLongClick: (Marker) -> Unit = {},
) {
    val mapApplier = currentComposer.applier as? MapApplier
    ComposeNode<MarkerNode, MapApplier>(
        factory = {
            val marker = mapApplier?.map?.addMarker {
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
            marker.tag = tag
            MarkerNode(
                marker = marker,
                markerDragState = markerDragState,
                onMarkerClick = onClick,
                onInfoWindowClick = onInfoWindowClick,
                onInfoWindowClose = onInfoWindowClose,
                onInfoWindowLongClick = onInfoWindowLongClick,
            )
        },
        update = {
            update(markerDragState) { this.markerDragState = it }
            update(onClick) { this.onMarkerClick = it }
            update(onInfoWindowClick) { this.onInfoWindowClick = it }
            update(onInfoWindowClose) { this.onInfoWindowClose = it }
            update(onInfoWindowLongClick) { this.onInfoWindowLongClick = it }

            set(alpha) { this.marker.alpha = it }
            set(anchor) { this.marker.setAnchor(it.x, it.y) }
            set(draggable) { this.marker.isDraggable = it }
            set(flat) { this.marker.isFlat = it }
            set(icon) { this.marker.setIcon(it) }
            set(infoWindowAnchor) { this.marker.setInfoWindowAnchor(it.x, it.y) }
            set(position) { this.marker.position = it }
            set(rotation) { this.marker.rotation = it }
            set(snippet) { this.marker.snippet = it }
            set(tag) { this.marker.tag = it }
            set(title) { this.marker.title = it }
            set(visible) { this.marker.isVisible = it }
            set(zIndex) { this.marker.zIndex = it }
        }
    )
}