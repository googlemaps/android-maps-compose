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
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.geometry.Offset
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.GroundOverlay
import com.google.android.gms.maps.model.GroundOverlayOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.ktx.addGroundOverlay
import kotlin.IllegalStateException

internal class GroundOverlayNode(
    val groundOverlay: GroundOverlay,
    var onGroundOverlayClick: (GroundOverlay) -> Unit
) : MapNode {
    override fun onRemoved() {
        groundOverlay.remove()
    }
}

/**
 * The position of a [GroundOverlay].
 *
 * Use one of the [create] methods to construct an instance of this class.
 */
public class GroundOverlayPosition private constructor(
    public val latLngBounds: LatLngBounds? = null,
    public val location: LatLng? = null,
    public val width: Float? = null,
    public val height: Float? = null,
) {
    public companion object {
        public fun create(latLngBounds: LatLngBounds) : GroundOverlayPosition {
            return GroundOverlayPosition(latLngBounds = latLngBounds)
        }

        public fun create(location: LatLng, width: Float, height: Float? = null) : GroundOverlayPosition {
            return GroundOverlayPosition(
                location = location,
                width = width,
                height = height
            )
        }
    }
}

/**
 * A composable for a ground overlay on the map.
 *
 * @param position the position of the ground overlay
 * @param image the image of the ground overlay
 * @param anchor the anchor of the ground overlay
 * @param bearing the bearing of the ground overlay in degrees clockwise from north
 * @param clickable boolean indicating if the ground overlay is clickable or not
 * @param tag optional tag to associate with the ground overlay
 * @param transparency the transparency of the ground overlay
 * @param visible the visibility of the ground overlay
 * @param zIndex the z-index of the ground overlay
 * @param onClick a lambda invoked when the ground overlay is clicked
 */
@Composable
@GoogleMapComposable
public fun GroundOverlay(
    position: GroundOverlayPosition,
    image: BitmapDescriptor,
    anchor: Offset = Offset(0.5f, 0.5f),
    bearing: Float = 0f,
    clickable: Boolean = false,
    tag: Any? = null,
    transparency: Float = 0f,
    visible: Boolean = true,
    zIndex: Float = 0f,
    onClick: (GroundOverlay) -> Unit = {},
) {
    val mapApplier = currentComposer.applier as? MapApplier
    ComposeNode<GroundOverlayNode, MapApplier>(
        factory = {
            val groundOverlay = mapApplier?.map?.addGroundOverlay {
                anchor(anchor.x, anchor.y)
                bearing(bearing)
                clickable(clickable)
                image(image)
                position(position)
                transparency(transparency)
                visible(visible)
                zIndex(zIndex)
            } ?: error("Error adding ground overlay")
            groundOverlay.tag = tag
            GroundOverlayNode(groundOverlay, onClick)
        },
        update = {
            update(onClick) { this.onGroundOverlayClick = it }

            set(bearing) { this.groundOverlay.bearing = it }
            set(clickable) { this.groundOverlay.isClickable = it }
            set(image) { this.groundOverlay.setImage(it) }
            set(position) { this.groundOverlay.position(it) }
            set(tag) { this.groundOverlay.tag = it }
            set(transparency) { this.groundOverlay.transparency = it }
            set(visible) { this.groundOverlay.isVisible = it }
            set(zIndex) { this.groundOverlay.zIndex = it }
        }
    )
}

private fun GroundOverlay.position(position: GroundOverlayPosition) {
    if (position.latLngBounds != null) {
        setPositionFromBounds(position.latLngBounds)
        return
    }

    if (position.location != null) {
        setPosition(position.location)
    }

    if (position.width != null && position.height == null) {
        setDimensions(position.width)
    } else if (position.width != null && position.height != null) {
        setDimensions(position.width, position.height)
    }
}

private fun GroundOverlayOptions.position(position: GroundOverlayPosition): GroundOverlayOptions {
    if (position.latLngBounds != null) {
        return positionFromBounds(position.latLngBounds)
    }

    if (position.location == null || position.width == null) {
        throw IllegalStateException("Invalid position $position")
    }

    if (position.height == null) {
        return position(position.location, position.width)
    }

    return position(position.location, position.width, position.height)
}