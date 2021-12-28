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

import android.graphics.Point
import android.util.Size
import androidx.annotation.Px
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

@Immutable
enum class MapCameraState {
    IDLE, MOVE_CANCELED, MOVING, MOVE_STARTED
}

/**
 * A state object that can be hoisted to control and observe the map's camera state.
 *
 * This should be created via [_root_ide_package_.com.google.maps.android.compose.rememberCameraPositionState()].
 *
 * @param initialPosition the initial camera position
 */
class CameraPositionState(val initialPosition: CameraPosition) : ControllableCameraPositionState {
    /**
     * State of the camera.
     */
    var cameraState: MapCameraState by mutableStateOf(MapCameraState.IDLE)
        internal set

    /**
     * Current position of the camera on the map.
     */
    var cameraPosition: CameraPosition by mutableStateOf(initialPosition)
        internal set

    internal var animated: Boolean = true

    // This camera update is modified only internally and is used for performing camera movements
    // and animations
    internal var internalCameraUpdate: CameraUpdate by mutableStateOf(
        CameraUpdateFactory.newCameraPosition(initialPosition)
    )

    override fun zoomIn(animated: Boolean) {
        this.animated = animated
        internalCameraUpdate = CameraUpdateFactory.zoomIn()
    }

    override fun zoomOut(animated: Boolean) {
        this.animated = animated
        internalCameraUpdate = CameraUpdateFactory.zoomOut()
    }

    override fun zoomTo(zoom: Float, animated: Boolean) {
        this.animated = animated
        internalCameraUpdate = CameraUpdateFactory.zoomTo(zoom)
    }

    override fun zoomBy(amount: Float, focus: Point?, animated: Boolean) {
        this.animated = animated
        internalCameraUpdate = if (focus == null) {
            CameraUpdateFactory.zoomBy(amount)
        } else {
            CameraUpdateFactory.zoomBy(amount, focus)
        }
    }

    override fun scrollBy(@Px xPx: Float, @Px yPx: Float, animated: Boolean) {
        this.animated = animated
        internalCameraUpdate = CameraUpdateFactory.scrollBy(xPx, yPx)
    }

    override fun newCameraPosition(cameraPosition: CameraPosition, animated: Boolean) {
        this.animated = animated
        internalCameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition)
    }

    override fun newLatLng(latLng: LatLng, animated: Boolean) {
        this.animated = animated
        internalCameraUpdate = CameraUpdateFactory.newLatLng(latLng)
    }

    override fun newLatLngBounds(
        bounds: LatLngBounds,
        @Px paddingPx: Int,
        boundingBoxPx: Size?,
        animated: Boolean
    ) {
        this.animated = animated
        internalCameraUpdate = if (boundingBoxPx == null) {
            CameraUpdateFactory.newLatLngBounds(bounds, paddingPx)
        } else {
            CameraUpdateFactory.newLatLngBounds(
                bounds,
                boundingBoxPx.width,
                boundingBoxPx.height,
                paddingPx
            )
        }
    }

    companion object {
        /**
         * The default saver implementation for [CameraPositionState]
         */
        val Saver = run {
            val cameraPositionKey = "CameraPosition"
            mapSaver(
                save = {
                    mapOf(cameraPositionKey to it.cameraPosition)
                },
                restore = {
                    CameraPositionState(it[cameraPositionKey] as CameraPosition)
                }
            )
        }
    }
}

/**
 * An interface definition for an object with a controllable camera. This acts as a proxy for
 * factory methods in [CameraUpdateFactory].
 */
interface ControllableCameraPositionState {
    fun zoomIn(animated: Boolean = true)

    fun zoomOut(animated: Boolean = true)

    fun zoomTo(zoom: Float, animated: Boolean = true)

    fun zoomBy(amount: Float, focus: Point? = null, animated: Boolean = true)

    fun scrollBy(@Px xPx: Float, @Px yPx: Float, animated: Boolean = true)

    fun newCameraPosition(cameraPosition: CameraPosition, animated: Boolean = true)

    fun newLatLng(latLng: LatLng, animated: Boolean = true)

    fun newLatLngBounds(
        bounds: LatLngBounds,
        @Px paddingPx: Int,
        boundingBoxPx: Size? = null,
        animated: Boolean = true
    )
}

/**
 * Creates a [CameraPositionState] that is remembered across compositions and configurations.
 *
 * @param initialPosition the initial position of the camera on the map.
 */
@Composable
fun rememberCameraPositionState(
    initialPosition: CameraPosition =
        CameraPosition(LatLng(0.0, 0.0), 0f, 0f, 0f)
): CameraPositionState =
    rememberSaveable(saver = CameraPositionState.Saver) {
        CameraPositionState(initialPosition = initialPosition)
    }
