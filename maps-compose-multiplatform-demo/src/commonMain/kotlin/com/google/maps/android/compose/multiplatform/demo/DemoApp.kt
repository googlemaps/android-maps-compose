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

package com.google.maps.android.compose.multiplatform.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A demo of maps-compose-multiplatform, shown the same way on Android and iOS. */
public class MultiplatformDemo internal constructor(
    public val title: String,
    public val description: String,
    internal val content: @Composable () -> Unit,
)

/** Every demo, in the order the demo list shows them. */
public val multiplatformDemos: List<MultiplatformDemo> = listOf(
    MultiplatformDemo("Camera", "Move the camera from code and follow the user's gestures") {
        CameraDemo()
    },
    MultiplatformDemo("Markers", "Markers with info windows, a draggable marker and taps") {
        MarkersDemo()
    },
    MultiplatformDemo("Shapes", "A polyline, a polygon with a hole and a circle, all tappable") {
        ShapesDemo()
    },
    MultiplatformDemo("Clustering", "300 markers that cluster and split as you zoom") {
        ClusteringDemo()
    },
    MultiplatformDemo("Map settings", "Map types, traffic, buildings and gestures") {
        MapSettingsDemo()
    },
    MultiplatformDemo("Map clicks", "Tap to drop a marker, long press to clear them") {
        MapClicksDemo()
    },
)

/**
 * The demo list and the selected demo.
 *
 * @param startDemo the index of a demo to open first, or null to start on the list
 */
@Composable
public fun MultiplatformDemoApp(startDemo: Int? = null) {
    var selected by rememberSaveable { mutableStateOf(startDemo) }
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val demo = selected?.let { multiplatformDemos.getOrNull(it) }
        if (demo == null) {
            Header("Multiplatform demos")
            DemoList(onDemoClick = { selected = it })
        } else {
            Header(demo.title, onBack = { selected = null })
            Box(Modifier.fillMaxSize()) {
                demo.content()
            }
        }
    }
}

@Composable
private fun Header(title: String, onBack: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            BasicText(
                "‹ Demos",
                Modifier.clickable(onClick = onBack).padding(end = 16.dp),
                style = TextStyle(color = Accent, fontSize = 17.sp),
            )
        }
        BasicText(title, style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun DemoList(onDemoClick: (Int) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(multiplatformDemos.size) { index ->
            val demo = multiplatformDemos[index]
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { onDemoClick(index) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                BasicText(demo.title, style = TextStyle(fontSize = 17.sp))
                BasicText(demo.description, style = TextStyle(fontSize = 14.sp, color = Color.Gray))
            }
        }
    }
}

/** A small button drawn with foundation only, so the demos need no design system. */
@Composable
internal fun DemoButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    BasicText(
        text,
        modifier
            .background(Accent, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        style = TextStyle(color = Color.White, fontSize = 14.sp),
    )
}

/** A line of text over the map, readable on any map type. */
@Composable
internal fun DemoLabel(text: String, modifier: Modifier = Modifier) {
    BasicText(
        text,
        modifier
            .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        style = TextStyle(fontSize = 14.sp),
    )
}

@Composable
internal fun DemoControls(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.padding(12.dp)) {
        content()
        Spacer(Modifier.height(4.dp))
    }
}

internal val Accent = Color(0xFF1A73E8)
