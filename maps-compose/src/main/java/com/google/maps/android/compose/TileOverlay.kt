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
import com.google.android.gms.maps.model.GroundOverlay
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileProvider
import com.google.maps.android.ktx.addGroundOverlay
import com.google.maps.android.ktx.addTileOverlay

internal data class TileOverlayNode(
    var tileOverlay: TileOverlay,
    var onTileOverlayClick: (TileOverlay) -> Unit
)

/**
 * A composable for a tile overlay on the map.
 */
@Composable
fun GoogleMapScope.TileOverlay(
    tileProvider: TileProvider,
    fadeIn: Boolean = true,
    transparency: Float = 0f,
    visible: Boolean = true,
    zIndex: Float = 0f,
    onClick: (TileOverlay) -> Unit = {},
) {
    if (currentComposer.applier !is MapApplier) error("Invalid Applier.")
    val mapApplier = currentComposer.applier as MapApplier
    ComposeNode<TileOverlayNode, MapApplier>(
        factory = {
            val tileOverlay = mapApplier.map.addTileOverlay {
                tileProvider(tileProvider)
                fadeIn(fadeIn)
                transparency(transparency)
                visible(visible)
                zIndex(zIndex)
            } ?: error("Could not add tile overlay")
            TileOverlayNode(tileOverlay, onClick)
        },
        update = {
            set(onClick) { this.onTileOverlayClick = it }

            set(tileProvider) {
                this.tileOverlay.remove()
                this.tileOverlay = mapApplier.map.addTileOverlay {
                    tileProvider(tileProvider)
                    fadeIn(fadeIn)
                    transparency(transparency)
                    visible(visible)
                    zIndex(zIndex)
                } ?: error("Could not add tile overlay")
            }
            set(fadeIn) { this.tileOverlay.fadeIn = it }
            set(transparency) { this.tileOverlay.transparency = it }
            set(visible) { this.tileOverlay.isVisible = it }
            set(zIndex) { this.tileOverlay.zIndex = it }
        }
    )
}