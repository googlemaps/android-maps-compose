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

package com.google.maps.android.compose.snippets

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme

/**
 * Dedicated Activity class hosting the [BasicMapSnippet] showing minimum map setup.
 * Launchable via ADB:
 * `adb shell am start -n com.google.maps.android.compose.snippets/.BasicMapActivity`
 */
class BasicMapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { BasicMapSnippet() } }
    }
}

/**
 * Dedicated Activity class hosting the [CustomConfigMapSnippet] showing satellite and custom UI settings.
 * Launchable via ADB:
 * `adb shell am start -n com.google.maps.android.compose.snippets/.CustomConfigMapActivity`
 */
class CustomConfigMapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { CustomConfigMapSnippet() } }
    }
}

/**
 * Dedicated Activity class hosting the [MoveCameraSnippet] showing instant camera update.
 * Launchable via ADB:
 * `adb shell am start -n com.google.maps.android.compose.snippets/.MoveCameraActivity`
 */
class MoveCameraActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { MoveCameraSnippet() } }
    }
}

/**
 * Dedicated Activity class hosting the [AnimateCameraSnippet] showing animated camera update.
 * Launchable via ADB:
 * `adb shell am start -n com.google.maps.android.compose.snippets/.AnimateCameraActivity`
 */
class AnimateCameraActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { AnimateCameraSnippet() } }
    }
}

/**
 * Dedicated Activity class hosting the [RestrictCameraBoundsSnippet] showing bounding box restrictions.
 * Launchable via ADB:
 * `adb shell am start -n com.google.maps.android.compose.snippets/.RestrictCameraBoundsActivity`
 */
class RestrictCameraBoundsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { RestrictCameraBoundsSnippet() } }
    }
}

/**
 * Dedicated Activity class hosting the [BasicMarkerSnippet] showing a simple map pin.
 * Launchable via ADB:
 * `adb shell am start -n com.google.maps.android.compose.snippets/.BasicMarkerActivity`
 */
class BasicMarkerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { BasicMarkerSnippet() } }
    }
}

/**
 * Dedicated Activity class hosting the [CustomMarkerIconSnippet] showing customized HUE marker icons.
 * Launchable via ADB:
 * `adb shell am start -n com.google.maps.android.compose.snippets/.CustomMarkerIconActivity`
 */
class CustomMarkerIconActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { CustomMarkerIconSnippet() } }
    }
}

/**
 * Dedicated Activity class hosting the [MarkerComposableSnippet] showing fully custom Compose layouts as pins.
 * Launchable via ADB:
 * `adb shell am start -n com.google.maps.android.compose.snippets/.MarkerComposableActivity`
 */
class MarkerComposableActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { MarkerComposableSnippet() } }
    }
}

/**
 * Dedicated Activity class hosting the [CustomInfoWindowSnippet] showing custom pop-up InfoWindows.
 * Launchable via ADB:
 * `adb shell am start -n com.google.maps.android.compose.snippets/.CustomInfoWindowActivity`
 */
class CustomInfoWindowActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { CustomInfoWindowSnippet() } }
    }
}

/**
 * Dedicated Activity class hosting the [PolylineSnippet] showing vector polyline paths.
 * Launchable via ADB:
 * `adb shell am start -n com.google.maps.android.compose.snippets/.PolylineActivity`
 */
class PolylineActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { PolylineSnippet() } }
    }
}

/**
 * Dedicated Activity class hosting the [PolygonSnippet] showing solid filled vector shapes.
 * Launchable via ADB:
 * `adb shell am start -n com.google.maps.android.compose.snippets/.PolygonActivity`
 */
class PolygonActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { PolygonSnippet() } }
    }
}

/**
 * Dedicated Activity class hosting the [CircleSnippet] showing geographic circle vector areas.
 * Launchable via ADB:
 * `adb shell am start -n com.google.maps.android.compose.snippets/.CircleActivity`
 */
class CircleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { CircleSnippet() } }
    }
}

/**
 * Dedicated Activity class hosting the [MarkerClusteringSnippet] showing dynamic marker grouping.
 * Launchable via ADB:
 * `adb shell am start -n com.google.maps.android.compose.snippets/.MarkerClusteringActivity`
 */
class MarkerClusteringActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { MarkerClusteringSnippet() } }
    }
}

/**
 * Dedicated Activity class hosting the [GeoJsonLayerSnippet] showing GeoJSON layer rendering.
 * Launchable via ADB:
 * `adb shell am start -n com.google.maps.android.compose.snippets/.GeoJsonLayerActivity`
 */
class GeoJsonLayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { GeoJsonLayerSnippet() } }
    }
}

/**
 * Dedicated Activity class hosting the [KmlLayerSnippet] showing KML layer rendering.
 * Launchable via ADB:
 * `adb shell am start -n com.google.maps.android.compose.snippets/.KmlLayerActivity`
 */
class KmlLayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { KmlLayerSnippet() } }
    }
}

/**
 * Dedicated Activity class hosting the [GroundOverlaySnippet] showing flat image overlays.
 * Launchable via ADB:
 * `adb shell am start -n com.google.maps.android.compose.snippets/.GroundOverlayActivity`
 */
class GroundOverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { GroundOverlaySnippet() } }
    }
}

/**
 * Dedicated Activity class hosting the [TileOverlaySnippet] showing custom raster tile overlays.
 * Launchable via ADB:
 * `adb shell am start -n com.google.maps.android.compose.snippets/.TileOverlayActivity`
 */
class TileOverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { TileOverlaySnippet() } }
    }
}

/**
 * Dedicated Activity class hosting the [WmsTileOverlaySnippet] showing remote WMS map tile layers.
 * Launchable via ADB:
 * `adb shell am start -n com.google.maps.android.compose.snippets/.WmsTileOverlayActivity`
 */
class WmsTileOverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { WmsTileOverlaySnippet() } }
    }
}

/**
 * Dedicated Activity class hosting the [RememberComposeBitmapDescriptorSnippet] showing Composable marker icons.
 * Launchable via ADB:
 * `adb shell am start -n com.google.maps.android.compose.snippets/.RememberComposeBitmapDescriptorActivity`
 */
class RememberComposeBitmapDescriptorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { RememberComposeBitmapDescriptorSnippet() } }
    }
}

/**
 * Dedicated Activity class hosting the [ScaleBarSnippet] showing dynamic map distance scales.
 * Launchable via ADB:
 * `adb shell am start -n com.google.maps.android.compose.snippets/.ScaleBarActivity`
 */
class ScaleBarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { ScaleBarSnippet() } }
    }
}
