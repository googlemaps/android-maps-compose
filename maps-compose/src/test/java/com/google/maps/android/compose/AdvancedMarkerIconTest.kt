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

import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.PinConfig
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

public class AdvancedMarkerIconTest {

    private val icon = BitmapDescriptor(fakeWrapper("icon"))
    private val pinIcon = BitmapDescriptor(fakeWrapper("pin"))
    private val pinConfig = PinConfig.builder().build()

    private fun resolve(pinConfig: PinConfig?, icon: BitmapDescriptor?) =
        advancedMarkerIcon(pinConfig, icon) { pinIcon }

    @Test
    public fun testPinConfigWithoutIconUsesPinConfig() {
        assertSame(pinIcon, resolve(pinConfig, icon = null))
    }

    @Test
    public fun testPinConfigTakesPrecedenceOverIcon() {
        assertSame(pinIcon, resolve(pinConfig, icon))
    }

    @Test
    public fun testIconIsUsedWithoutPinConfig() {
        assertSame(icon, resolve(pinConfig = null, icon))
    }

    @Test
    public fun testNoPinConfigAndNoIconResolvesToDefault() {
        assertNull(resolve(pinConfig = null, icon = null))
    }
}
