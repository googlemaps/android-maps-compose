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

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.Stable
import androidx.compose.runtime.currentComposer
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PatternItem
import com.google.android.gms.maps.model.Polygon
import com.google.maps.android.ktx.addPolygon

@Stable
internal data class PolygonNode(
    val polygon: Polygon,
    var onPolygonClick: (Polygon) -> Unit
)

@Composable
fun GoogleMapScope.Polygon(
    points: List<LatLng>,
    clickable: Boolean = false,
    @ColorInt fillColor: Int = Color.BLACK,
    geodesic: Boolean = false,
    holes: List<List<LatLng>> = emptyList(),
    @ColorInt strokeColor: Int = Color.BLACK,
    strokeJointType: Int = JointType.DEFAULT,
    strokePattern: List<PatternItem>? = null,
    strokeWidth: Float = 10f,
    visible: Boolean = true,
    zIndex: Float = 0f,
    onClick: (Polygon) -> Unit = {}
) {
    if (currentComposer.applier !is MapApplier) error("Invalid Applier.")
    val mapApplier = currentComposer.applier as MapApplier
    ComposeNode<PolygonNode, MapApplier>(
        factory = {
            val polygon = mapApplier.map.addPolygon {
                addAll(points)
                clickable(clickable)
                fillColor(fillColor)
                geodesic(geodesic)
                holes.forEach {
                    addHole(it)
                }
                strokeColor(strokeColor)
                strokeJointType(strokeJointType)
                strokePattern(strokePattern)
                strokeWidth(strokeWidth)
                visible(visible)
                zIndex(zIndex)
            }
            PolygonNode(polygon, onClick)
        },
        update = {
            set(onClick) { this.onPolygonClick = it }

            set(points) { this.polygon.points = it }
            set(clickable) { this.polygon.isClickable = it }
            set(fillColor) { this.polygon.fillColor = it }
            set(geodesic) { this.polygon.isGeodesic = it }
            set(holes) { this.polygon.holes = it }
            set(strokeColor) { this.polygon.strokeColor = it }
            set(strokeJointType) { this.polygon.strokeJointType = it }
            set(strokePattern) { this.polygon.strokePattern = it }
            set(strokeWidth) { this.polygon.strokeWidth = it }
            set(visible) { this.polygon.isVisible = it }
            set(zIndex) { this.polygon.zIndex = it }
        }
    )
}