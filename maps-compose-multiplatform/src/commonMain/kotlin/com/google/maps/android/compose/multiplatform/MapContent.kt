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

package com.google.maps.android.compose.multiplatform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.google.maps.android.model.LatLng

/**
 * The position of a [Marker], hoisted so that it can be moved from code and, for a draggable
 * marker, follow the user's drag.
 */
@Stable
public class MarkerState(position: LatLng) {
    /** The marker's position. Updated while the user drags a draggable marker. */
    public var position: LatLng by mutableStateOf(position)
}

/** Creates and remembers a [MarkerState] at [position]. */
@Composable
public fun rememberMarkerState(position: LatLng): MarkerState = remember { MarkerState(position) }

/**
 * A marker on the map, shown with the platform's default pin. Tapping it shows an info window
 * with [title] and [snippet], unless [onClick] returns true.
 *
 * Must be called from the content of a [GoogleMap].
 *
 * @param color tints the platform's default pin, or null for its standard red
 * @param onClick called when the marker is tapped. Return true to consume the tap and skip the
 * default behavior of centering the marker and showing its info window.
 */
@Composable
public fun Marker(
    state: MarkerState,
    title: String? = null,
    snippet: String? = null,
    color: Color? = null,
    alpha: Float = 1f,
    draggable: Boolean = false,
    flat: Boolean = false,
    rotation: Float = 0f,
    visible: Boolean = true,
    zIndex: Float = 0f,
    onClick: (MarkerState) -> Boolean = { false },
) {
    val node = rememberNode { MarkerNode(state) }
    SideEffect {
        node.state = state
        node.title = title
        node.snippet = snippet
        node.color = color
        node.alpha = alpha
        node.draggable = draggable
        node.flat = flat
        node.rotation = rotation
        node.visible = visible
        node.zIndex = zIndex
        node.onClick = onClick
    }
}

/**
 * A line through [points].
 *
 * @param width the line width in density-independent pixels, so it looks the same on Android and
 * iOS
 * @param geodesic whether each segment follows the shortest path on the Earth's surface instead
 * of a straight line on the map
 * @param onClick called when the line is tapped; only when [clickable]
 */
@Composable
public fun Polyline(
    points: List<LatLng>,
    color: Color = Color.Black,
    width: Float = DEFAULT_STROKE_WIDTH,
    geodesic: Boolean = false,
    clickable: Boolean = false,
    visible: Boolean = true,
    zIndex: Float = 0f,
    onClick: () -> Unit = {},
) {
    val node = rememberNode { PolylineNode() }
    SideEffect {
        node.points = points
        node.color = color
        node.width = width
        node.geodesic = geodesic
        node.clickable = clickable
        node.visible = visible
        node.zIndex = zIndex
        node.onClick = onClick
    }
}

/**
 * A filled shape with its outline at [points], minus any [holes].
 *
 * @param strokeWidth the outline width in density-independent pixels
 * @param onClick called when the polygon is tapped; only when [clickable]
 */
@Composable
public fun Polygon(
    points: List<LatLng>,
    holes: List<List<LatLng>> = emptyList(),
    fillColor: Color = Color.Black,
    strokeColor: Color = Color.Black,
    strokeWidth: Float = DEFAULT_STROKE_WIDTH,
    geodesic: Boolean = false,
    clickable: Boolean = false,
    visible: Boolean = true,
    zIndex: Float = 0f,
    onClick: () -> Unit = {},
) {
    val node = rememberNode { PolygonNode() }
    SideEffect {
        node.points = points
        node.holes = holes
        node.fillColor = fillColor
        node.strokeColor = strokeColor
        node.strokeWidth = strokeWidth
        node.geodesic = geodesic
        node.clickable = clickable
        node.visible = visible
        node.zIndex = zIndex
        node.onClick = onClick
    }
}

/**
 * A circle of [radius] meters around [center].
 *
 * @param strokeWidth the outline width in density-independent pixels
 * @param onClick called when the circle is tapped; only when [clickable]
 */
@Composable
public fun Circle(
    center: LatLng,
    radius: Double,
    fillColor: Color = Color.Transparent,
    strokeColor: Color = Color.Black,
    strokeWidth: Float = DEFAULT_STROKE_WIDTH,
    clickable: Boolean = false,
    visible: Boolean = true,
    zIndex: Float = 0f,
    onClick: () -> Unit = {},
) {
    val node = rememberNode { CircleNode(center) }
    SideEffect {
        node.center = center
        node.radius = radius
        node.fillColor = fillColor
        node.strokeColor = strokeColor
        node.strokeWidth = strokeWidth
        node.clickable = clickable
        node.visible = visible
        node.zIndex = zIndex
        node.onClick = onClick
    }
}

/**
 * Something drawn on the map. Each content composable owns one node for as long as it is in the
 * composition, so the platform map keeps one native object per node and only updates what
 * changed. Properties are snapshot state, so reading them in the platform map's update observes
 * them.
 */
internal sealed class MapNode {
    var visible by mutableStateOf(true)
    var zIndex by mutableStateOf(0f)
}

internal class MarkerNode(state: MarkerState) : MapNode() {
    var state by mutableStateOf(state)
    var title by mutableStateOf<String?>(null)
    var snippet by mutableStateOf<String?>(null)
    var color by mutableStateOf<Color?>(null)
    var alpha by mutableStateOf(1f)
    var draggable by mutableStateOf(false)
    var flat by mutableStateOf(false)
    var rotation by mutableStateOf(0f)
    var onClick: (MarkerState) -> Boolean = { false }
}

internal class PolylineNode : MapNode() {
    var points by mutableStateOf(emptyList<LatLng>())
    var color by mutableStateOf(Color.Black)
    var width by mutableStateOf(10f)
    var geodesic by mutableStateOf(false)
    var clickable by mutableStateOf(false)
    var onClick: () -> Unit = {}
}

internal class PolygonNode : MapNode() {
    var points by mutableStateOf(emptyList<LatLng>())
    var holes by mutableStateOf(emptyList<List<LatLng>>())
    var fillColor by mutableStateOf(Color.Black)
    var strokeColor by mutableStateOf(Color.Black)
    var strokeWidth by mutableStateOf(10f)
    var geodesic by mutableStateOf(false)
    var clickable by mutableStateOf(false)
    var onClick: () -> Unit = {}
}

internal class CircleNode(center: LatLng) : MapNode() {
    var center by mutableStateOf(center)
    var radius by mutableStateOf(0.0)
    var fillColor by mutableStateOf(Color.Transparent)
    var strokeColor by mutableStateOf(Color.Black)
    var strokeWidth by mutableStateOf(10f)
    var clickable by mutableStateOf(false)
    var onClick: () -> Unit = {}
}

/** The nodes of one map, in the order they entered the composition. */
internal class MapNodeRegistry {
    val nodes = mutableStateListOf<MapNode>()
}

/** About the width of the platforms' own default lines, in density-independent pixels. */
private const val DEFAULT_STROKE_WIDTH = 4f

internal val LocalMapNodeRegistry = staticCompositionLocalOf<MapNodeRegistry?> { null }

internal val LocalCameraPositionState = staticCompositionLocalOf<CameraPositionState?> { null }

/** Remembers a node and keeps it in the enclosing map's registry while in the composition. */
@Composable
private inline fun <T : MapNode> rememberNode(crossinline create: () -> T): T {
    val registry = checkNotNull(LocalMapNodeRegistry.current) {
        "Map content such as Marker must be called from the content of a GoogleMap."
    }
    val node = remember { create() }
    DisposableEffect(registry, node) {
        registry.nodes.add(node)
        onDispose { registry.nodes.remove(node) }
    }
    return node
}
