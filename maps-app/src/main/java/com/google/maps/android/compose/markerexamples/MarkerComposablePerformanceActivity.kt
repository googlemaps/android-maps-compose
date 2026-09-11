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

package com.google.maps.android.compose.markerexamples

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.google.maps.android.compose.singapore
import com.google.maps.android.compose.theme.MapsComposeSampleTheme
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Data model for a dynamic marker rendered using [MarkerComposable].
 */
@Immutable
data class PerformanceMarkerItem(
    val id: Int,
    val position: LatLng,
    val counter: Int,
    val colorIndex: Int
)

private val badgeColors = listOf(
    Color(0xFF1E88E5), // Blue
    Color(0xFF43A047), // Green
    Color(0xFFE53935), // Red
    Color(0xFFFB8C00), // Orange
    Color(0xFF8E24AA), // Purple
    Color(0xFF00ACC1), // Cyan
)

/**
 * Demonstrates high-performance rendering and rapid state updates with [MarkerComposable].
 * Shows how ComposeView reuse and non-blocking bitmap creation eliminate UI jank when
 * displaying 25-100 dynamically updating markers.
 */
class MarkerComposablePerformanceActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MapsComposeSampleTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PerformanceScreen()
                }
            }
        }
    }
}

@OptIn(MapsComposeExperimentalApi::class)
@Composable
private fun PerformanceScreen() {
    var markerCount by remember { mutableIntStateOf(30) }
    var updateTick by remember { mutableIntStateOf(0) }
    var isAutoUpdating by remember { mutableStateOf(false) }
    var isFastUpdating by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    // Generate initial grid/cluster of markers around Singapore center
    val markers = remember(markerCount, updateTick) {
        val center = singapore
        (0 until markerCount).map { i ->
            val angle = (i * 2.0 * Math.PI) / markerCount
            val radius = 0.02 + (i % 4) * 0.015
            val lat = center.latitude + radius * sin(angle)
            val lng = center.longitude + (radius * 1.3) * cos(angle)
            PerformanceMarkerItem(
                id = i + 1,
                position = LatLng(lat, lng),
                counter = updateTick + i,
                colorIndex = (i + updateTick) % badgeColors.size
            )
        }
    }

    // Auto-update ticker effect
    LaunchedEffect(isAutoUpdating, isFastUpdating) {
        while (isAutoUpdating || isFastUpdating) {
            val interval = if (isFastUpdating) 250L else 1000L
            delay(interval)
            updateTick++
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(singapore, 12f)
    }

    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = remember { MapProperties(mapType = MapType.NORMAL) },
            uiSettings = remember { MapUiSettings(zoomControlsEnabled = false) }
        ) {
            markers.forEach { item ->
                val markerState = rememberUpdatedMarkerState(position = item.position)
                MarkerComposable(
                    keys = arrayOf(item.id, item.counter, item.colorIndex),
                    state = markerState,
                    title = "Marker #${item.id}",
                    snippet = "Count: ${item.counter}"
                ) {
                    MarkerBadgeComposable(item = item)
                }
            }
        }

        // Top Header Info Card with Info Action
        Card(
            onClick = { showInfoDialog = true },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "MarkerComposable Performance",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$markerCount markers • Ticks: $updateTick • Tap for info",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { showInfoDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Demo Info",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Explanation Dialog
        if (showInfoDialog) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text(
                        text = "About MarkerComposable Demo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "This screen demonstrates the rendering performance of MarkerComposable when rendering multiple custom Compose-based markers with high-frequency dynamic updates.",
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        HorizontalDivider()

                        Text(
                            text = "📍 What the Numbers Mean",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Left Circle (1–75): The Marker ID, uniquely identifying each marker in the collection.\n" +
                                   "• Right Value (⚡ Bolt): The Live State Counter. Each time the state updates, this increments to prove real-time offscreen re-rasterization.\n" +
                                   "• Badge Color: Cycles dynamically with state updates.",
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        HorizontalDivider()

                        Text(
                            text = "🎛️ Interactive Controls",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Markers (10 / 25 / 50 / 75): Select how many markers are rendered concurrently.\n" +
                                   "• Auto (1Hz): Automatically triggers state updates once every second.\n" +
                                   "• Stress (4Hz): Stress-tests the UI thread with 4 full multi-marker state updates per second.\n" +
                                   "• +1 Button: Manually steps all marker states forward by 1.",
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        HorizontalDivider()

                        Text(
                            text = "⚡ Under the Hood Optimization",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Instead of creating and destroying ComposeView instances for every render, each marker retains a single ComposeView across its lifecycle. Rasterization is deferred off the synchronous composition pass into LaunchedEffect, eliminating UI jank.",
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showInfoDialog = false }) {
                        Text("Got it")
                    }
                }
            )
        }

        // Bottom Control Panel
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Marker Count Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Markers:", fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(10, 25, 50, 75).forEach { count ->
                            FilterChip(
                                selected = markerCount == count,
                                onClick = { markerCount = count },
                                label = { Text("$count") }
                            )
                        }
                    }
                }

                // Action Buttons & Auto-update switches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Switch(
                            checked = isAutoUpdating,
                            onCheckedChange = {
                                isAutoUpdating = it
                                if (it) isFastUpdating = false
                            }
                        )
                        Text(text = "Auto (1Hz)", fontSize = 13.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Switch(
                            checked = isFastUpdating,
                            onCheckedChange = {
                                isFastUpdating = it
                                if (it) isAutoUpdating = false
                            }
                        )
                        Text(text = "Stress (4Hz)", fontSize = 13.sp)
                    }

                    Button(
                        onClick = { updateTick++ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Update",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "+1", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/**
 * Custom Composable rendered inside [MarkerComposable] offscreen.
 */
@Composable
private fun MarkerBadgeComposable(item: PerformanceMarkerItem) {
    val targetColor = badgeColors[item.colorIndex]
    val backgroundColor by animateColorAsState(targetValue = targetColor, label = "badgeColor")

    Box(
        modifier = Modifier
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(2.dp, Color.White, RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${item.id}",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${item.counter}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
    }
}
