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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.theme.MapsComposeSampleTheme
import kotlinx.coroutines.launch

/**
 * Theme selection options for the sample.
 */
enum class ThemeOption {
    SYSTEM,
    LIGHT,
    DARK
}

/**
 * Demonstrates GoogleMap in Lite Mode following the system theme setting, camera movements,
 * and custom map styling.
 *
 * ### Following System Theme in Lite Mode:
 * The underlying Google Maps Android SDK serves static pre-rendered tiles in Lite Mode and does
 * not support dynamic [com.google.android.gms.maps.model.MapColorScheme].
 *
 * To have a Lite Mode map follow the system setting:
 * 1. Listen for system theme changes using [isSystemInDarkTheme].
 * 2. Dynamically apply a custom dark JSON style via [MapProperties.mapStyleOptions] when dark theme is active.
 *
 * For a complete guide on this technique, see [Lite Mode Dark Theme Guide](../../../../../../../../../docs/lite-mode-dark-theme.md).
 */
class LiteModeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var selectedTheme by rememberSaveable { mutableStateOf(ThemeOption.SYSTEM) }
            val systemInDarkTheme = isSystemInDarkTheme()
            val isDark = when (selectedTheme) {
                ThemeOption.SYSTEM -> systemInDarkTheme
                ThemeOption.LIGHT -> false
                ThemeOption.DARK -> true
            }

            MapsComposeSampleTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LiteModeScreen(
                        selectedTheme = selectedTheme,
                        onSelectTheme = { selectedTheme = it },
                        isDark = isDark,
                        systemInDarkTheme = systemInDarkTheme
                    )
                }
            }
        }
    }
}

/**
 * Minimal dark JSON style for Lite Mode maps.
 */
private const val DARK_STYLE_JSON = """[
  {"elementType": "geometry", "stylers": [{"color": "#242f3e"}]},
  {"elementType": "labels.text.stroke", "stylers": [{"color": "#242f3e"}]},
  {"elementType": "labels.text.fill", "stylers": [{"color": "#746855"}]},
  {"featureType": "administrative.locality", "elementType": "labels.text.fill", "stylers": [{"color": "#d59563"}]},
  {"featureType": "poi", "elementType": "labels.text.fill", "stylers": [{"color": "#d59563"}]},
  {"featureType": "poi.park", "elementType": "geometry", "stylers": [{"color": "#263c3f"}]},
  {"featureType": "poi.park", "elementType": "labels.text.fill", "stylers": [{"color": "#6b9a76"}]},
  {"featureType": "road", "elementType": "geometry", "stylers": [{"color": "#38414e"}]},
  {"featureType": "road", "elementType": "geometry.stroke", "stylers": [{"color": "#212a37"}]},
  {"featureType": "road", "elementType": "labels.text.fill", "stylers": [{"color": "#9ca5b3"}]},
  {"featureType": "road.highway", "elementType": "geometry", "stylers": [{"color": "#746855"}]},
  {"featureType": "water", "elementType": "geometry", "stylers": [{"color": "#17263c"}]},
  {"featureType": "water", "elementType": "labels.text.fill", "stylers": [{"color": "#515c6d"}]}
]"""

@Composable
fun LiteModeScreen(
    selectedTheme: ThemeOption,
    onSelectTheme: (ThemeOption) -> Unit,
    isDark: Boolean,
    systemInDarkTheme: Boolean,
) {
    val singapore = remember { LatLng(1.35, 103.87) }
    val tokyo = remember { LatLng(35.6895, 139.6917) }
    val coroutineScope = rememberCoroutineScope()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(singapore, 11f)
    }

    var selectedMapType by rememberSaveable { mutableStateOf(MapType.NORMAL) }

    // Dynamically apply the dark JSON style to the Lite map whenever dark mode is active
    val mapProperties = remember(selectedMapType, isDark) {
        MapProperties(
            mapType = selectedMapType,
            mapStyleOptions = if (isDark) MapStyleOptions(DARK_STYLE_JSON) else null
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = {
                coroutineScope.launch {
                    val newTarget = if (cameraPositionState.position.target == singapore) tokyo else singapore
                    cameraPositionState.animate(CameraUpdateFactory.newLatLng(newTarget))
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Animate Camera")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Map Type:", style = MaterialTheme.typography.labelLarge)
            FilterChip(
                selected = selectedMapType == MapType.NORMAL,
                onClick = { selectedMapType = MapType.NORMAL },
                label = { Text("Normal") }
            )
            FilterChip(
                selected = selectedMapType == MapType.TERRAIN,
                onClick = { selectedMapType = MapType.TERRAIN },
                label = { Text("Terrain") }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Theme:", style = MaterialTheme.typography.labelLarge)
            FilterChip(
                selected = selectedTheme == ThemeOption.SYSTEM,
                onClick = { onSelectTheme(ThemeOption.SYSTEM) },
                label = { Text("System") }
            )
            FilterChip(
                selected = selectedTheme == ThemeOption.LIGHT,
                onClick = { onSelectTheme(ThemeOption.LIGHT) },
                label = { Text("Light") }
            )
            FilterChip(
                selected = selectedTheme == ThemeOption.DARK,
                onClick = { onSelectTheme(ThemeOption.DARK) },
                label = { Text("Dark") }
            )
        }

        Text(
            text = "Active: ${if (isDark) "Dark" else "Light"}" +
                if (selectedTheme == ThemeOption.SYSTEM) " (System: ${if (systemInDarkTheme) "Dark" else "Light"})" else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                googleMapOptionsFactory = {
                    GoogleMapOptions()
                        .liteMode(true)
                        .mapType(selectedMapType.value)
                },
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
            )
        }
    }
}
