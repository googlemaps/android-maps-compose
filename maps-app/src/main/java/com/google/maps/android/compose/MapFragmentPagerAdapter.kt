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
