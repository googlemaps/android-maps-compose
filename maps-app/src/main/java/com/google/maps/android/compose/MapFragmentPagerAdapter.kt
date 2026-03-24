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

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.gms.maps.model.LatLng

class MapFragmentPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    private val mapConfigs = listOf(
        MapConfig( // First map - Los Angeles
            initialLatLng = LatLng(34.0522, -118.2437),
            initialZoom = 10f,
            title = "Los Angeles",
            // LA gets the custom content marker
            markerType = MarkerType.CUSTOM_CONTENT_MARKER
        ),
        MapConfig( // Second map - New York City
            initialLatLng = LatLng(40.7128, -74.0060),
            initialZoom = 10f,
            title = "New York City",
            // NYC gets the standard marker
            markerType = MarkerType.STANDARD_MARKER_WITH_SNIPPET,
            standardMarkerSnippet = "The Big Apple!"
        )
    )

    override fun getItemCount(): Int = mapConfigs.size

    override fun createFragment(position: Int): Fragment {
        return GoogleMapComposeFragment.newInstance(mapConfigs[position])
    }
}
