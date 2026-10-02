/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.maps.android.compose.multiplatform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.google.maps.android.model.CameraPosition
import com.google.maps.android.model.LatLng
import com.google.maps.android.model.bearing
import com.google.maps.android.model.latitude
import com.google.maps.android.model.longitude
import com.google.maps.android.model.target
import com.google.maps.android.model.tilt
import com.google.maps.android.model.zoom

/**
 * State object that can be hoisted to control and observe the camera of a [GoogleMap].
 *
 * The state follows the map both ways: setting [position] or calling [animate] moves the map, and
 * the user's gestures update [position] and [isMoving].
 *
 * @param position the initial camera position, used until the map reports its own
 */
public class CameraPositionState(
    position: CameraPosition = CameraPosition(LatLng(0.0, 0.0), 0f, 0f, 0f),
) {
    private var rawPosition by mutableStateOf(position)

    /** Whether the camera is currently moving, from a gesture or an animation. */
    public var isMoving: Boolean by mutableStateOf(false)
        internal set

    /**
     * The current camera position. Setting it moves the camera immediately; use [animate] to
     * move it gradually.
     */
    public var position: CameraPosition
        get() = rawPosition
        set(value) {
            val controller = controller
            if (controller == null) rawPosition = value else controller.move(value)
        }

    /** Set by the map while it is shown, so that moves reach the platform map. */
    internal var controller: CameraController? = null

    /**
     * Animates the camera to [position] over [durationMs] milliseconds, suspending until the
     * animation ends. Without a map attached, the position is applied immediately.
     */
    public suspend fun animate(position: CameraPosition, durationMs: Int = DEFAULT_ANIMATION_MS) {
        val controller = controller
        if (controller == null) rawPosition = position else controller.animate(position, durationMs)
    }

    /** Called by the platform map when its camera changes, without moving it again. */
    internal fun onCameraChanged(position: CameraPosition, isMoving: Boolean) {
        rawPosition = position
        this.isMoving = isMoving
    }

    public companion object {
        internal const val DEFAULT_ANIMATION_MS: Int = 1_000

        /** Saves and restores the camera position across configuration and process changes. */
        public val Saver: Saver<CameraPositionState, Any> = listSaver(
            save = {
                val position = it.position
                listOf(
                    position.target.latitude,
                    position.target.longitude,
                    position.zoom.toDouble(),
                    position.tilt.toDouble(),
                    position.bearing.toDouble(),
                )
            },
            restore = { values ->
                CameraPositionState(
                    CameraPosition(
                        LatLng(values[0], values[1]),
                        values[2].toFloat(),
                        values[3].toFloat(),
                        values[4].toFloat(),
                    )
                )
            },
        )
    }
}

/** Moves the platform map's camera on behalf of a [CameraPositionState]. */
internal interface CameraController {
    fun move(position: CameraPosition)

    suspend fun animate(position: CameraPosition, durationMs: Int)
}

/**
 * Creates and remembers a [CameraPositionState], saved across configuration changes.
 *
 * @param init applied once to the new state, for example to set its initial [position]
 */
@Composable
public inline fun rememberCameraPositionState(
    crossinline init: CameraPositionState.() -> Unit = {},
): CameraPositionState = rememberSaveable(saver = CameraPositionState.Saver) {
    CameraPositionState().apply(init)
}

/** Shorthand for a camera position looking straight down at [target]. */
public fun cameraPosition(target: LatLng, zoom: Float): CameraPosition =
    CameraPosition(target, zoom, 0f, 0f)
