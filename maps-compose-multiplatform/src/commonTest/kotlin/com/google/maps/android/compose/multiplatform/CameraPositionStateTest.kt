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

import androidx.compose.runtime.saveable.SaverScope
import com.google.maps.android.model.CameraPosition
import com.google.maps.android.model.LatLng
import com.google.maps.android.model.bearing
import com.google.maps.android.model.target
import com.google.maps.android.model.tilt
import com.google.maps.android.model.zoom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class CameraPositionStateTest {
    private val start = CameraPosition(LatLng(37.7749, -122.4194), 12f, 30f, 90f)
    private val end = cameraPosition(LatLng(51.5007, -0.1246), 14f)

    /** Records moves the way a platform map would receive them. */
    private class FakeController : CameraController {
        val moves = mutableListOf<CameraPosition>()
        val animations = mutableListOf<Pair<CameraPosition, Int>>()

        override fun move(position: CameraPosition) {
            moves += position
        }

        override suspend fun animate(position: CameraPosition, durationMs: Int) {
            animations += position to durationMs
        }
    }

    @Test
    fun withoutAMapSettingThePositionAppliesIt() {
        val state = CameraPositionState(start)

        state.position = end

        assertSame(end, state.position)
    }

    @Test
    fun withAMapSettingThePositionMovesTheMapInstead() {
        val state = CameraPositionState(start)
        val controller = FakeController()
        state.controller = controller

        state.position = end

        assertEquals(listOf(end), controller.moves)
        // The position follows once the map reports its new camera.
        assertSame(start, state.position)
        state.onCameraChanged(end, isMoving = false)
        assertSame(end, state.position)
    }

    @Test
    fun animateGoesThroughTheMap() = runTest {
        val state = CameraPositionState(start)
        val controller = FakeController()
        state.controller = controller

        state.animate(end, durationMs = 250)

        assertEquals(listOf(end to 250), controller.animations)
    }

    @Test
    fun theMapReportsWhetherTheCameraIsMoving() {
        val state = CameraPositionState(start)

        state.onCameraChanged(end, isMoving = true)
        assertTrue(state.isMoving)

        state.onCameraChanged(end, isMoving = false)
        assertFalse(state.isMoving)
    }

    @Test
    fun theSaverRestoresEveryCameraProperty() {
        val scope = SaverScope { true }
        val saved = with(CameraPositionState.Saver) { scope.save(CameraPositionState(start)) }

        val restored = assertNotNull(CameraPositionState.Saver.restore(assertNotNull(saved)))

        val position = restored.position
        assertEquals(start.target, position.target)
        assertEquals(start.zoom, position.zoom)
        assertEquals(start.tilt, position.tilt)
        assertEquals(start.bearing, position.bearing)
    }

    @Test
    fun mapPropertiesCompareByValue() {
        val properties = MapProperties(mapType = MapType.SATELLITE)

        assertEquals(properties, MapProperties(mapType = MapType.SATELLITE))
        assertEquals(MapType.HYBRID, properties.copy(mapType = MapType.HYBRID).mapType)
        assertEquals(MapUiSettings(zoomGesturesEnabled = false), MapUiSettings().copy(zoomGesturesEnabled = false))
    }
}
