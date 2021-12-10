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

import androidx.compose.runtime.AbstractApplier
import com.google.android.gms.maps.GoogleMap

internal class MapApplier(
    val map: GoogleMap
) : AbstractApplier<Any?>(null) {

    private val decorations = mutableListOf<Any?>()

    init {
        map.setOnMarkerClickListener { marker ->
            val node = decorations.first {
                it is MarkerNode && it.marker == marker
            } as? MarkerNode
            node?.onMarkerClick?.invoke(marker) ?: false
        }
        map.setOnCircleClickListener { circle ->
            val node = decorations.first {
                it is CircleNode && it.circle == circle
            } as? CircleNode
            node?.onCircleClick?.invoke(circle)
        }
    }

    override fun onClear() {
        map.clear()
    }

    override fun insertBottomUp(index: Int, instance: Any?) {
        decorations.add(index, instance)
    }

    override fun insertTopDown(index: Int, instance: Any?) {
        // insertBottomUp is preferred
    }

    override fun move(from: Int, to: Int, count: Int) {
        decorations.move(from, to, count)
    }

    override fun remove(index: Int, count: Int) {
        repeat(count) {
            when (val decoration = decorations[index + it]) {
                is MarkerNode -> decoration.marker.remove()
                is CircleNode -> decoration.circle.remove()
            }
        }
        decorations.remove(index, count)
    }
}

