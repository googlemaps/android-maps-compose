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

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
const val timeout2 = 2_000L
const val timeout3 = 3_000L
const val timeout5 = 5_000L
const val MAP_LOAD_TIMEOUT_SECONDS = 30L

val hasValidApiKey: Boolean =
    BuildConfig.MAPS_API_KEY.isNotBlank() && BuildConfig.MAPS_API_KEY != "YOUR_API_KEY"

const val assertRoundingError: Double = 0.01

fun LatLng.assertEquals(other: LatLng) {
    assertEquals(latitude, other.latitude, assertRoundingError)
    assertEquals(longitude, other.longitude, assertRoundingError)
}


fun ComposeMapColorScheme.assertEquals(other: ComposeMapColorScheme) {
    assertEquals(other, this)
}