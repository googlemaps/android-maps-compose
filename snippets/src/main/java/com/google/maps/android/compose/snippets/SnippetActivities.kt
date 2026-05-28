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
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Google Developers Brand inspired Light Color Scheme (Clean White & Google Blue). Completely
 * replaces the default Material 3 lavender/purple seed color theme.
 */
val MapsComposeColorScheme =
  lightColorScheme(
    primary = Color(0xFF1A73E8), // Google Blue
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE8F0FE), // Translucent Blue
    onPrimaryContainer = Color(0xFF1A73E8),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF202124),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF202124),
    surfaceVariant = Color(0xFFF8F9FA), // Light Gray for Card backgrounds
    onSurfaceVariant = Color(0xFF3C4043)
  )

/**
 * Base Activity class that automatically hides the system status bar and navigation bar to provide
 * a clean, edge-to-edge full-screen bleed layout suitable for professional screenshots.
 */
abstract class BaseSnippetActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
  }
}

/**
 * Dedicated Activity class hosting the [BasicMapSnippet] showing minimum map setup. Launchable via
 * ADB: `adb shell am start -n com.google.maps.android.compose.snippets/.BasicMapActivity`
 */
class BasicMapActivity : BaseSnippetActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { MaterialTheme(colorScheme = MapsComposeColorScheme) { BasicMapSnippet() } }
  }
}

/**
 * Dedicated Activity class hosting the [CustomConfigMapSnippet] showing satellite and custom UI
 * settings. Launchable via ADB: `adb shell am start -n
 * com.google.maps.android.compose.snippets/.CustomConfigMapActivity`
 */
class CustomConfigMapActivity : BaseSnippetActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { MaterialTheme(colorScheme = MapsComposeColorScheme) { CustomConfigMapSnippet() } }
  }
}

/**
 * Dedicated Activity class hosting the [MoveCameraSnippet] showing instant camera update.
 * Launchable via ADB: `adb shell am start -n
 * com.google.maps.android.compose.snippets/.MoveCameraActivity`
 */
class MoveCameraActivity : BaseSnippetActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { MaterialTheme(colorScheme = MapsComposeColorScheme) { MoveCameraSnippet() } }
  }
}

/**
 * Dedicated Activity class hosting the [AnimateCameraSnippet] showing animated camera update.
 * Launchable via ADB: `adb shell am start -n
 * com.google.maps.android.compose.snippets/.AnimateCameraActivity`
 */
class AnimateCameraActivity : BaseSnippetActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { MaterialTheme(colorScheme = MapsComposeColorScheme) { AnimateCameraSnippet() } }
  }
}

/**
 * Dedicated Activity class hosting the [RestrictCameraBoundsSnippet] showing bounding box
 * restrictions. Launchable via ADB: `adb shell am start -n
 * com.google.maps.android.compose.snippets/.RestrictCameraBoundsActivity`
 */
class RestrictCameraBoundsActivity : BaseSnippetActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      MaterialTheme(colorScheme = MapsComposeColorScheme) { RestrictCameraBoundsSnippet() }
    }
  }
}

/**
 * Dedicated Activity class hosting the [BasicMarkerSnippet] showing a simple map pin. Launchable
 * via ADB: `adb shell am start -n com.google.maps.android.compose.snippets/.BasicMarkerActivity`
 */
class BasicMarkerActivity : BaseSnippetActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { MaterialTheme(colorScheme = MapsComposeColorScheme) { BasicMarkerSnippet() } }
  }
}

/**
 * Dedicated Activity class hosting the [CustomMarkerIconSnippet] showing customized HUE marker
 * icons. Launchable via ADB: `adb shell am start -n
 * com.google.maps.android.compose.snippets/.CustomMarkerIconActivity`
 */
class CustomMarkerIconActivity : BaseSnippetActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { MaterialTheme(colorScheme = MapsComposeColorScheme) { CustomMarkerIconSnippet() } }
  }
}

/**
 * Dedicated Activity class hosting the [MarkerComposableSnippet] showing fully custom Compose
 * layouts as pins. Launchable via ADB: `adb shell am start -n
 * com.google.maps.android.compose.snippets/.MarkerComposableActivity`
 */
class MarkerComposableActivity : BaseSnippetActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { MaterialTheme(colorScheme = MapsComposeColorScheme) { MarkerComposableSnippet() } }
  }
}

/**
 * Dedicated Activity class hosting the [CustomInfoWindowSnippet] showing custom pop-up InfoWindows.
 * Launchable via ADB: `adb shell am start -n
 * com.google.maps.android.compose.snippets/.CustomInfoWindowActivity`
 */
class CustomInfoWindowActivity : BaseSnippetActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { MaterialTheme(colorScheme = MapsComposeColorScheme) { CustomInfoWindowSnippet() } }
  }
}

/**
 * Dedicated Activity class hosting the [PolylineSnippet] showing vector polyline paths. Launchable
 * via ADB: `adb shell am start -n com.google.maps.android.compose.snippets/.PolylineActivity`
 */
class PolylineActivity : BaseSnippetActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { MaterialTheme(colorScheme = MapsComposeColorScheme) { PolylineSnippet() } }
  }
}

/**
 * Dedicated Activity class hosting the [PolygonSnippet] showing solid filled vector shapes.
 * Launchable via ADB: `adb shell am start -n
 * com.google.maps.android.compose.snippets/.PolygonActivity`
 */
class PolygonActivity : BaseSnippetActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { MaterialTheme(colorScheme = MapsComposeColorScheme) { PolygonSnippet() } }
  }
}

/**
 * Dedicated Activity class hosting the [CircleSnippet] showing geographic circle vector areas.
 * Launchable via ADB: `adb shell am start -n
 * com.google.maps.android.compose.snippets/.CircleActivity`
 */
class CircleActivity : BaseSnippetActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { MaterialTheme(colorScheme = MapsComposeColorScheme) { CircleSnippet() } }
  }
}

/**
 * Dedicated Activity class hosting the [MarkerClusteringSnippet] showing dynamic marker grouping.
 * Launchable via ADB: `adb shell am start -n
 * com.google.maps.android.compose.snippets/.MarkerClusteringActivity`
 */
class MarkerClusteringActivity : BaseSnippetActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { MaterialTheme(colorScheme = MapsComposeColorScheme) { MarkerClusteringSnippet() } }
  }
}

/**
 * Dedicated Activity class hosting the [GeoJsonLayerSnippet] showing GeoJSON layer rendering.
 * Launchable via ADB: `adb shell am start -n
 * com.google.maps.android.compose.snippets/.GeoJsonLayerActivity`
 */
class GeoJsonLayerActivity : BaseSnippetActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { MaterialTheme(colorScheme = MapsComposeColorScheme) { GeoJsonLayerSnippet() } }
  }
}

/**
 * Dedicated Activity class hosting the [KmlLayerSnippet] showing KML layer rendering. Launchable
 * via ADB: `adb shell am start -n com.google.maps.android.compose.snippets/.KmlLayerActivity`
 */
class KmlLayerActivity : BaseSnippetActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { MaterialTheme(colorScheme = MapsComposeColorScheme) { KmlLayerSnippet() } }
  }
}

/**
 * Dedicated Activity class hosting the [GroundOverlaySnippet] showing flat image overlays.
 * Launchable via ADB: `adb shell am start -n
 * com.google.maps.android.compose.snippets/.GroundOverlayActivity`
 */
class GroundOverlayActivity : BaseSnippetActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { MaterialTheme(colorScheme = MapsComposeColorScheme) { GroundOverlaySnippet() } }
  }
}

/**
 * Dedicated Activity class hosting the [TileOverlaySnippet] showing custom raster tile overlays.
 * Launchable via ADB: `adb shell am start -n
 * com.google.maps.android.compose.snippets/.TileOverlayActivity`
 */
class TileOverlayActivity : BaseSnippetActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { MaterialTheme(colorScheme = MapsComposeColorScheme) { TileOverlaySnippet() } }
  }
}

/**
 * Dedicated Activity class hosting the [WmsTileOverlaySnippet] showing remote WMS map tile layers.
 * Launchable via ADB: `adb shell am start -n
 * com.google.maps.android.compose.snippets/.WmsTileOverlayActivity`
 */
class WmsTileOverlayActivity : BaseSnippetActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { MaterialTheme(colorScheme = MapsComposeColorScheme) { WmsTileOverlaySnippet() } }
  }
}

/**
 * Dedicated Activity class hosting the [RememberComposeBitmapDescriptorSnippet] showing Composable
 * marker icons. Launchable via ADB: `adb shell am start -n
 * com.google.maps.android.compose.snippets/.RememberComposeBitmapDescriptorActivity`
 */
class RememberComposeBitmapDescriptorActivity : BaseSnippetActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      MaterialTheme(colorScheme = MapsComposeColorScheme) {
        RememberComposeBitmapDescriptorSnippet()
      }
    }
  }
}

/**
 * Dedicated Activity class hosting the [ScaleBarSnippet] showing dynamic map distance scales.
 * Launchable via ADB: `adb shell am start -n
 * com.google.maps.android.compose.snippets/.ScaleBarActivity`
 */
class ScaleBarActivity : BaseSnippetActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { MaterialTheme(colorScheme = MapsComposeColorScheme) { ScaleBarSnippet() } }
  }
}
