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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.android.gms.maps.model.ButtCap
import com.google.android.gms.maps.model.Cap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PatternItem
import com.google.android.gms.maps.model.Polyline
import com.google.maps.android.ktx.addPolyline

internal class PolylineNode(
    val polyline: Polyline,
    var onPolylineClick: (Polyline) -> Unit
) : MapNode {
    override fun onRemoved() {
        polyline.remove()
    }
}

/**
 * A composable for a polyline on the map.
 *
 * @param points the points comprising the polyline
 * @param clickable boolean indicating if the polyline is clickable or not
 * @param color the color of the polyline
 * @param endCap a cap at the end vertex of the polyline
 * @param geodesic specifies whether to draw the polyline as a geodesic
 * @param jointType the joint type for all vertices of the polyline except the start and end
 * vertices
 * @param pattern the pattern for the polyline
 * @param startCap the cap at the start vertex of the polyline
 * @param visible the visibility of the polyline
 * @param width the width of the polyline in screen pixels
 * @param zIndex the z-index of the polyline
 * @param onClick a lambda invoked when the polyline is clicked
 */
@Composable
@GoogleMapComposable
public fun Polyline(
    points: List<LatLng>,
    clickable: Boolean = false,
    color: Color = Color.Black,
    endCap: Cap = ButtCap(),
    geodesic: Boolean = false,
    jointType: Int = JointType.DEFAULT,
    pattern: List<PatternItem>? = null,
    startCap: Cap = ButtCap(),
    tag: Any? = null,
    visible: Boolean = true,
    width: Float = 10f,
    zIndex: Float = 0f,
    onClick: (Polyline) -> Unit = {}
) {
    val mapApplier = currentComposer.applier as MapApplier?
    ComposeNode<PolylineNode, MapApplier>(
        factory = {
            val polyline = mapApplier?.map?.addPolyline {
                addAll(points)
                clickable(clickable)
                color(color.toArgb())
                endCap(endCap)
                geodesic(geodesic)
                jointType(jointType)
                pattern(pattern)
                startCap(startCap)
                visible(visible)
                width(width)
                zIndex(zIndex)
            } ?: error("Error adding Polyline")
            polyline.tag = tag
            PolylineNode(polyline, onClick)
        },
        update = {
            update(onClick) { this.onPolylineClick = it }

            set(points) { this.polyline.points = it }
            set(clickable) { this.polyline.isClickable = it }
            set(color) { this.polyline.color = it.toArgb() }
            set(endCap) { this.polyline.endCap = it }
            set(geodesic) { this.polyline.isGeodesic = it }
            set(jointType) { this.polyline.jointType = it }
            set(pattern) { this.polyline.pattern = it }
            set(startCap) { this.polyline.startCap = it }
            set(tag) { this.polyline.tag = it }
            set(visible) { this.polyline.isVisible = it }
            set(width) { this.polyline.width = it }
            set(zIndex) { this.polyline.zIndex = it }
        }
    )
}