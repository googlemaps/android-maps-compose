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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileProvider
import com.google.maps.android.ktx.addTileOverlay

private class TileOverlayNode(
    var tileOverlay: TileOverlay,
    var tileOverlayState: TileOverlayState,
    var onTileOverlayClick: (TileOverlay) -> Unit
) : MapNode {
    override fun onAttached() {
        tileOverlayState.tileOverlay = tileOverlay
    }
    override fun onRemoved() {
        tileOverlay.remove()
    }
}

@Composable
@GoogleMapComposable
@Deprecated("For compatibility", level = DeprecationLevel.HIDDEN)
public fun TileOverlay(
    tileProvider: TileProvider,
    fadeIn: Boolean = true,
    transparency: Float = 0f,
    visible: Boolean = true,
    zIndex: Float = 0f,
    onClick: (TileOverlay) -> Unit = {},
) {
    TileOverlay(
        tileProvider = tileProvider,
        state = rememberTileOverlayState(),
        fadeIn = fadeIn,
        transparency = transparency,
        visible = visible,
        zIndex = zIndex,
        onClick = onClick,
    )
}

/**
 * A composable for a tile overlay on the map.
 *
 * @param tileProvider the tile provider to use for this tile overlay
 * @param state the [TileOverlayState] to be used to control the tile overlay, such as clearing
 * stale tiles
 * @param fadeIn boolean indicating whether the tiles should fade in
 * @param transparency the transparency of the tile overlay
 * @param visible the visibility of the tile overlay
 * @param zIndex the z-index of the tile overlay
 * @param onClick a lambda invoked when the tile overlay is clicked
 */
@Composable
@GoogleMapComposable
public fun TileOverlay(
    tileProvider: TileProvider,
    state: TileOverlayState = rememberTileOverlayState(),
    fadeIn: Boolean = true,
    transparency: Float = 0f,
    visible: Boolean = true,
    zIndex: Float = 0f,
    onClick: (TileOverlay) -> Unit = {},
) {
    val mapApplier = currentComposer.applier as MapApplier?
    ComposeNode<TileOverlayNode, MapApplier>(
        factory = {
            val tileOverlay = mapApplier?.map?.addTileOverlay {
                tileProvider(tileProvider)
                fadeIn(fadeIn)
                transparency(transparency)
                visible(visible)
                zIndex(zIndex)
            } ?: error("Error adding tile overlay")
            TileOverlayNode(tileOverlay, state, onClick)
        },
        update = {
            update(onClick) { this.onTileOverlayClick = it }

            set(tileProvider) {
                this.tileOverlay.remove()
                this.tileOverlay = mapApplier?.map?.addTileOverlay {
                    tileProvider(tileProvider)
                    fadeIn(fadeIn)
                    transparency(transparency)
                    visible(visible)
                    zIndex(zIndex)
                } ?: error("Error adding tile overlay")
            }
            set(fadeIn) { this.tileOverlay.fadeIn = it }
            set(transparency) { this.tileOverlay.transparency = it }
            set(visible) { this.tileOverlay.isVisible = it }
            set(zIndex) { this.tileOverlay.zIndex = it }
        }
    )
}

/**
 * A state object that can be hoisted to control the state of a [TileOverlay].
 * A [TileOverlayState] may only be used by a single [TileOverlay] composable at a time.
 *
 * [clearTileCache] can be called to request that the map refresh these tiles.
 */
public class TileOverlayState {

    internal var tileOverlay: TileOverlay? by mutableStateOf(null)

    /**
     * Call to force a refresh if the tiles provided by the tile overlay become 'stale'.
     * This will cause all the tiles on this overlay to be reloaded.
     * For example, if the tiles provided by the [TileProvider] change, you must call
     * this afterwards to ensure that the previous tiles are no longer rendered.
     *
     * See [Maps SDK docs](https://developers.google.com/maps/documentation/android-sdk/tileoverlay#clear)
     */
    public fun clearTileCache() {
        (tileOverlay ?: error("This TileOverlayState is not used in any TileOverlay"))
            .clearTileCache()
    }

}

@Composable
public fun rememberTileOverlayState(): TileOverlayState {
    return remember { TileOverlayState() }
}
