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

import androidx.compose.runtime.AbstractApplier
import com.google.android.gms.maps.StreetViewPanorama
import com.google.maps.android.compose.MapNode

private object StreetViewPanoramaNodeRoot : MapNode

internal class StreetViewPanoramaApplier(
    val streetViewPanorama: StreetViewPanorama
) : AbstractApplier<MapNode>(StreetViewPanoramaNodeRoot) {
    override fun onClear() { }

    override fun insertBottomUp(index: Int, instance: MapNode) {
        instance.onAttached()
    }

    override fun insertTopDown(index: Int, instance: MapNode) { }

    override fun move(from: Int, to: Int, count: Int) { }

    override fun remove(index: Int, count: Int) { }
}
