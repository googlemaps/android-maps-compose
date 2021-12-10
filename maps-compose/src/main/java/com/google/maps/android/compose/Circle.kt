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
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PatternItem
import com.google.maps.android.ktx.addCircle

@Stable
internal data class CircleNode(
    val circle: Circle,
    var onCircleClick: (Circle) -> Unit
)

/**
 * A composable for a circle on the map.
 */
@Composable
fun GoogleMapScope.Circle(
    center: LatLng,
    clickable: Boolean = false,
    @ColorInt fillColor: Int = Color.TRANSPARENT,
    radius: Double = 0.0,
    @ColorInt strokeColor: Int = Color.BLACK,
    strokePattern: List<PatternItem>? = null,
    strokeWidth: Float = 10f,
    visible: Boolean = true,
    zIndex: Float = 0f,
    onClick: (Circle) -> Unit = {},
) {
    if (currentComposer.applier !is MapApplier) error("Invalid Applier.")
    val mapApplier = currentComposer.applier as MapApplier
    ComposeNode<CircleNode, MapApplier>(
        factory = {
            val circle = mapApplier.map.addCircle {
                center(center)
                clickable(clickable)
                fillColor(fillColor)
                radius(radius)
                strokeColor(strokeColor)
                strokePattern(strokePattern)
                strokeWidth(strokeWidth)
                visible(visible)
                zIndex(zIndex)
            }
            CircleNode(circle, onClick)
        },
        update = {
            set(onClick) { this.onCircleClick = it }

            set(center) { this.circle.center = it }
            set(clickable) { this.circle.isClickable = it }
            set(fillColor) { this.circle.fillColor = it }
            set(radius) { this.circle.radius = it }
            set(strokeColor) { this.circle.strokeColor = it }
            set(strokePattern) { this.circle.strokePattern = it }
            set(strokeWidth) { this.circle.strokeWidth = it }
            set(visible) { this.circle.isVisible = it }
            set(zIndex) { this.circle.zIndex = it }
        }
    )
}