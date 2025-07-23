package com.google.maps.android.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset // For MarkerComposable anchor
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng


enum class MarkerType {
    CUSTOM_CONTENT_MARKER,        // To use com.google.maps.android.compose.MarkerComposable with your Text
    STANDARD_MARKER_WITH_SNIPPET  // To use the standard com.google.maps.android.compose.Marker
}

data class MapConfig(
    val initialLatLng: LatLng,
    val initialZoom: Float,
    val title: String,
    val markerType: MarkerType = MarkerType.CUSTOM_CONTENT_MARKER, // Default
    val standardMarkerSnippet: String? = null
)

class GoogleMapComposeFragment : Fragment() {

    private var mapConfig: MapConfig? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val lat = it.getDouble(ARG_LAT, Double.NaN)
            val lng = it.getDouble(ARG_LNG, Double.NaN)
            val zoom = it.getFloat(ARG_ZOOM, 10f)
            val title = it.getString(ARG_TITLE, "Map")

            val markerTypeName = it.getString(ARG_MARKER_TYPE)
            val markerType = markerTypeName?.let { name ->
                try {
                    MarkerType.valueOf(name)
                } catch (e: IllegalArgumentException) {
                    MarkerType.CUSTOM_CONTENT_MARKER
                }
            } ?: MarkerType.CUSTOM_CONTENT_MARKER

            val snippet = it.getString(ARG_STANDARD_MARKER_SNIPPET)

            if (!lat.isNaN() && !lng.isNaN()) {
                mapConfig = MapConfig(
                    initialLatLng = LatLng(lat, lng),
                    initialZoom = zoom,
                    title = title,
                    markerType = markerType,
                    standardMarkerSnippet = snippet
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            MaterialTheme {
                val currentConfig = mapConfig ?: MapConfig(
                    initialLatLng = LatLng(0.0, 0.0),
                    initialZoom = 2f,
                    title = "Default Map",
                    markerType = MarkerType.CUSTOM_CONTENT_MARKER
                )
                MapContent(config = currentConfig)
            }
        }
    }

    @Composable
    fun MapContent(config: MapConfig) {
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(config.initialLatLng, config.initialZoom)
        }

        GoogleMap(
            cameraPositionState = cameraPositionState
        ) {
            when (config.markerType) {
                MarkerType.CUSTOM_CONTENT_MARKER -> {
                    val markerState = rememberUpdatedMarkerState(position = config.initialLatLng)
                    markerState.position = config.initialLatLng

                    MarkerComposable(
                        state = markerState,
                        anchor = Offset(0.5f, 1.0f)
                    ){
                        Text(text = "Hello, World! (from ${config.title})")
                    }
                }
                MarkerType.STANDARD_MARKER_WITH_SNIPPET -> {
                    val markerState = rememberUpdatedMarkerState(position = config.initialLatLng)
                    markerState.position = config.initialLatLng

                    Marker(
                        state = markerState,
                        title = config.title,
                        snippet = config.standardMarkerSnippet ?: "Standard Marker Snippet" // Snippet for the info window
                    )
                }
            }
        }
    }

    companion object {
        private const val ARG_LAT = "arg_lat"
        private const val ARG_LNG = "arg_lng"
        private const val ARG_ZOOM = "arg_zoom"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_MARKER_TYPE = "arg_marker_type"
        private const val ARG_STANDARD_MARKER_SNIPPET = "arg_standard_marker_snippet"

        @JvmStatic
        fun newInstance(config: MapConfig): GoogleMapComposeFragment {
            return GoogleMapComposeFragment().apply {
                arguments = Bundle().apply {
                    putDouble(ARG_LAT, config.initialLatLng.latitude)
                    putDouble(ARG_LNG, config.initialLatLng.longitude)
                    putFloat(ARG_ZOOM, config.initialZoom)
                    putString(ARG_TITLE, config.title)
                    putString(ARG_MARKER_TYPE, config.markerType.name)
                    config.standardMarkerSnippet?.let { putString(ARG_STANDARD_MARKER_SNIPPET, it) }
                }
            }
        }
    }
}
