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

package com.google.maps.android.compose

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

public class CameraPositionStateTest {

    private val explicitPosition = CameraPosition.fromLatLngZoom(LatLng(48.1, 11.5), 15f)

    @Before
    public fun setUp() {
        initFakeCameraUpdateFactory()
    }

    @Test
    public fun testBindingMapAppliesInitialCameraPosition() {
        val state = CameraPositionState(position = explicitPosition)
        val calls = mutableListOf<FakeCall>()

        state.setMap(fakeGoogleMap(calls))

        assertEquals(
            listOf(moveCameraTo(explicitPosition)),
            calls.filter { it.method == "moveCamera" },
        )
        assertEquals(explicitPosition, state.position)
    }

    @Test
    public fun testSettingPositionWhileUnboundOverridesPendingMove() {
        val state = CameraPositionState()
        state.move(CameraUpdateFactory.zoomTo(3f))
        state.position = explicitPosition

        val calls = mutableListOf<FakeCall>()
        state.setMap(fakeGoogleMap(calls))

        assertEquals(listOf(moveCameraTo(explicitPosition)), calls.filter { it.method == "moveCamera" })
    }

    @Test(timeout = 5_000)
    public fun testSettingPositionWhileUnboundCancelsPendingAnimate(): Unit = runBlocking {
        val state = CameraPositionState()
        val animation = launch(start = CoroutineStart.UNDISPATCHED) {
            state.animate(CameraUpdateFactory.zoomTo(3f))
        }
        state.position = explicitPosition
        animation.join()
        assertTrue(animation.isCancelled)

        val calls = mutableListOf<FakeCall>()
        state.setMap(fakeGoogleMap(calls))

        assertEquals(listOf(moveCameraTo(explicitPosition)), calls.filter { it.method == "moveCamera" })
    }

    @Test
    public fun testPendingMoveIsAppliedWhenMapIsBound() {
        val state = CameraPositionState()
        state.move(CameraUpdateFactory.zoomTo(3f))

        val calls = mutableListOf<FakeCall>()
        state.setMap(fakeGoogleMap(calls))

        assertEquals(
            listOf(
                moveCameraTo(state.position),
                FakeCall("moveCamera", listOf(FakeCall("zoomTo", listOf(3f)))),
            ),
            calls.filter { it.method == "moveCamera" },
        )
    }

    private fun moveCameraTo(position: CameraPosition) =
        FakeCall("moveCamera", listOf(FakeCall("newCameraPosition", listOf(position))))
}
