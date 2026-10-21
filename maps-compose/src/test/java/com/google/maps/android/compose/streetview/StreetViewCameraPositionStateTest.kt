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

package com.google.maps.android.compose.streetview

import com.google.android.gms.maps.StreetViewPanorama
import com.google.android.gms.maps.internal.IStreetViewPanoramaDelegate
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.StreetViewSource
import com.google.maps.android.compose.FakeCall
import com.google.maps.android.compose.fakeDelegate
import org.junit.Assert.assertEquals
import org.junit.Test

public class StreetViewCameraPositionStateTest {

    private val position = LatLng(48.1, 11.5)

    private fun setPositionCalls(block: StreetViewCameraPositionState.() -> Unit): List<FakeCall> {
        val calls = mutableListOf<FakeCall>()
        val state = StreetViewCameraPositionState()
        state.panorama = StreetViewPanorama(fakeDelegate<IStreetViewPanoramaDelegate>(calls))
        state.block()
        return calls.filter { it.method.startsWith("setPosition") }
    }

    @Test
    public fun testSourceWithoutRadiusIsForwarded() {
        assertEquals(
            listOf(FakeCall("setPositionWithSource", listOf(position, StreetViewSource.OUTDOOR))),
            setPositionCalls { setPosition(position, source = StreetViewSource.OUTDOOR) },
        )
    }

    @Test
    public fun testRadiusAndSourceAreForwarded() {
        assertEquals(
            listOf(
                FakeCall(
                    "setPositionWithRadiusAndSource",
                    listOf(position, 50, StreetViewSource.OUTDOOR),
                ),
            ),
            setPositionCalls { setPosition(position, radius = 50, source = StreetViewSource.OUTDOOR) },
        )
    }

    @Test
    public fun testPositionOnly() {
        assertEquals(
            listOf(FakeCall("setPosition", listOf(position))),
            setPositionCalls { setPosition(position) },
        )
    }
}
