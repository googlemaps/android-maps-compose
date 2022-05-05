// Copyright 2021 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.maps.android.compose

import androidx.compose.runtime.AbstractApplier
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.GroundOverlay
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.Polyline

internal interface MapNode {
    fun onAttached() {}
    fun onRemoved() {}
    fun onCleared() {}
}

private object MapNodeRoot : MapNode

internal class MapApplier(
    val map: GoogleMap,
    private val mapView: MapView,
) : AbstractApplier<MapNode>(MapNodeRoot) {

    private val decorations = mutableListOf<MapNode>()

    init {
        attachClickListeners()
    }

    override fun onClear() {
        map.clear()
        decorations.forEach { it.onCleared() }
        decorations.clear()
    }

    override fun insertBottomUp(index: Int, instance: MapNode) {
        decorations.add(index, instance)
        instance.onAttached()
    }

    override fun insertTopDown(index: Int, instance: MapNode) {
        // insertBottomUp is preferred
    }

    override fun move(from: Int, to: Int, count: Int) {
        decorations.move(from, to, count)
    }

    override fun remove(index: Int, count: Int) {
        repeat(count) {
            decorations[index + it].onRemoved()
        }
        decorations.remove(index, count)
    }

    private fun attachClickListeners() {
        map.setOnCircleClickListener {
            decorations.nodeForCircle(it)
                ?.onCircleClick
                ?.invoke(it)
        }
        map.setOnGroundOverlayClickListener {
            decorations.nodeForGroundOverlay(it)
                ?.onGroundOverlayClick
                ?.invoke(it)
        }
        map.setOnPolygonClickListener {
            decorations.nodeForPolygon(it)
                ?.onPolygonClick
                ?.invoke(it)
        }
        map.setOnPolylineClickListener {
            decorations.nodeForPolyline(it)
                ?.onPolylineClick
                ?.invoke(it)
        }

        // Marker
        map.setOnMarkerClickListener { marker ->
            decorations.nodeForMarker(marker)
                ?.onMarkerClick
                ?.invoke(marker)
                ?: false
        }
        map.setOnInfoWindowClickListener { marker ->
            decorations.nodeForMarker(marker)
                ?.onInfoWindowClick
                ?.invoke(marker)
        }
        map.setOnInfoWindowCloseListener { marker ->
            decorations.nodeForMarker(marker)
                ?.onInfoWindowClose
                ?.invoke(marker)
        }
        map.setOnInfoWindowLongClickListener { marker ->
            decorations.nodeForMarker(marker)
                ?.onInfoWindowLongClick
                ?.invoke(marker)
        }
        map.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDrag(marker: Marker) {
                with(decorations.nodeForMarker(marker)) {
                    this?.markerState?.position = marker.position
                    this?.markerState?.dragState = DragState.DRAG
                }
            }

            override fun onMarkerDragEnd(marker: Marker) {
                with(decorations.nodeForMarker(marker)) {
                    this?.markerState?.position = marker.position
                    this?.markerState?.dragState = DragState.END
                }
            }

            override fun onMarkerDragStart(marker: Marker) {
                with(decorations.nodeForMarker(marker)) {
                    this?.markerState?.position = marker.position
                    this?.markerState?.dragState = DragState.START
                }
            }
        })
        map.setInfoWindowAdapter(
            ComposeInfoWindowAdapter(
                mapView,
                markerNodeFinder = { decorations.nodeForMarker(it) }
            )
        )
    }
}

private fun MutableList<MapNode>.nodeForCircle(circle: Circle): CircleNode? =
    firstOrNull { it is CircleNode && it.circle == circle } as? CircleNode

private fun MutableList<MapNode>.nodeForMarker(marker: Marker): MarkerNode? =
    firstOrNull { it is MarkerNode && it.marker == marker } as? MarkerNode

private fun MutableList<MapNode>.nodeForPolygon(polygon: Polygon): PolygonNode? =
    firstOrNull { it is PolygonNode && it.polygon == polygon } as? PolygonNode

private fun MutableList<MapNode>.nodeForPolyline(polyline: Polyline): PolylineNode? =
    firstOrNull { it is PolylineNode && it.polyline == polyline } as? PolylineNode

private fun MutableList<MapNode>.nodeForGroundOverlay(
    groundOverlay: GroundOverlay
): GroundOverlayNode? =
    firstOrNull { it is GroundOverlayNode && it.groundOverlay == groundOverlay } as? GroundOverlayNode
