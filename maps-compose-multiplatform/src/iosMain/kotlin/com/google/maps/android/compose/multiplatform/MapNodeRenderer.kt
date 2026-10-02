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

import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.graphics.Color
import cocoapods.GoogleMaps.GMSCircle
import cocoapods.GoogleMaps.GMSMapView
import cocoapods.GoogleMaps.GMSMarker
import cocoapods.GoogleMaps.GMSMutablePath
import cocoapods.GoogleMaps.GMSOverlay
import cocoapods.GoogleMaps.GMSPath
import cocoapods.GoogleMaps.GMSPolygon
import cocoapods.GoogleMaps.GMSPolyline
import com.google.maps.android.model.LatLng
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIColor

/**
 * Keeps one native overlay per [MapNode] in [registry] on [mapView], creating, updating and
 * removing them as the nodes change.
 *
 * Every snapshot state read while syncing is observed, so a change to the node list or to any
 * node's properties schedules another sync on the main thread. Each sync only touches the
 * overlays' properties; overlays are never recreated, so markers keep their info windows and
 * drags.
 */
@OptIn(ExperimentalForeignApi::class)
internal class MapNodeRenderer(
    private val mapView: GMSMapView,
    private val registry: MapNodeRegistry,
) {
    private val overlays = mutableMapOf<MapNode, GMSOverlay>()
    private val nodesByOverlay = mutableMapOf<GMSOverlay, MapNode>()

    /** The color each marker's icon was drawn with, so the image is only rebuilt on a change. */
    private val markerColors = mutableMapOf<MarkerNode, Color?>()

    private val observer = SnapshotStateObserver { command ->
        NSOperationQueue.mainQueue.addOperationWithBlock(command)
    }
    private val onChanged: (MapNodeRenderer) -> Unit = { sync() }

    fun start() {
        observer.start()
        sync()
    }

    fun stop() {
        observer.stop()
        observer.clear()
        overlays.values.forEach { it.map = null }
        overlays.clear()
        nodesByOverlay.clear()
        markerColors.clear()
    }

    fun nodeFor(overlay: GMSOverlay): MapNode? = nodesByOverlay[overlay]

    fun onMarkerDragged(marker: GMSMarker) {
        val node = nodesByOverlay[marker] as? MarkerNode ?: return
        node.state.position = marker.position.toLatLng()
    }

    private fun sync() {
        observer.observeReads(this, onChanged) {
            val nodes = registry.nodes.toList()
            val removed = overlays.keys - nodes.toSet()
            removed.forEach { node ->
                overlays.remove(node)?.let { overlay ->
                    overlay.map = null
                    nodesByOverlay.remove(overlay)
                }
                markerColors.remove(node)
            }
            nodes.forEach { node ->
                val overlay = overlays.getOrPut(node) {
                    create(node).also { nodesByOverlay[it] = node }
                }
                update(node, overlay)
            }
        }
    }

    private fun create(node: MapNode): GMSOverlay = when (node) {
        is MarkerNode -> GMSMarker()
        is PolylineNode -> GMSPolyline()
        is PolygonNode -> GMSPolygon()
        is CircleNode -> GMSCircle()
    }

    private fun update(node: MapNode, overlay: GMSOverlay) {
        when (node) {
            is MarkerNode -> (overlay as GMSMarker).apply {
                position = node.state.position.toCoordinate()
                title = node.title
                snippet = node.snippet
                val color = node.color
                if (!markerColors.containsKey(node) || markerColors[node] != color) {
                    markerColors[node] = color
                    icon = color?.let { GMSMarker.markerImageWithColor(it.toUIColor()) }
                }
                opacity = node.alpha
                draggable = node.draggable
                flat = node.flat
                rotation = node.rotation.toDouble()
            }
            is PolylineNode -> (overlay as GMSPolyline).apply {
                path = node.points.toPath()
                strokeColor = node.color.toUIColor()
                // Points, which are density-independent like the shared API's widths.
                strokeWidth = node.width.toDouble()
                geodesic = node.geodesic
                tappable = node.clickable
            }
            is PolygonNode -> (overlay as GMSPolygon).apply {
                path = node.points.toPath()
                holes = node.holes.map { it.toPath() }
                fillColor = node.fillColor.toUIColor()
                strokeColor = node.strokeColor.toUIColor()
                strokeWidth = node.strokeWidth.toDouble()
                geodesic = node.geodesic
                tappable = node.clickable
            }
            is CircleNode -> (overlay as GMSCircle).apply {
                position = node.center.toCoordinate()
                radius = node.radius
                fillColor = node.fillColor.toUIColor()
                strokeColor = node.strokeColor.toUIColor()
                strokeWidth = node.strokeWidth.toDouble()
                tappable = node.clickable
            }
        }
        overlay.zIndex = node.zIndex.toInt()
        overlay.map = if (node.visible) mapView else null
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun List<LatLng>.toPath(): GMSPath = GMSMutablePath().also { path ->
    forEach { path.addCoordinate(it.toCoordinate()) }
}

private fun Color.toUIColor(): UIColor = UIColor.colorWithRed(
    red = red.toDouble(),
    green = green.toDouble(),
    blue = blue.toDouble(),
    alpha = alpha.toDouble(),
)
