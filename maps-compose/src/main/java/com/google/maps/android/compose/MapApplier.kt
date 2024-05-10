// Copyright 2023 Google LLC
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

// [mapClickListeners] must be a singleton for the [map] and is therefore stored here:
// [GoogleMap.setOnIndoorStateChangeListener()] will not actually set a new non-null listener if
// called more than once; if [mapClickListeners] were passed through the Compose function hierarchy
// we would need to consider the case of it changing, which would require special treatment
// for that particular listener; yet MapClickListeners never actually changes.
internal class MapApplier(
    val map: GoogleMap,
    internal val mapView: MapView,
    val mapClickListeners: MapClickListeners,
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

    internal fun attachClickListeners() {
        map.setOnCircleClickListener { circle ->
            decorations.findInputCallback<CircleNode, Circle, Unit>(
                nodeMatchPredicate = { it.circle == circle },
                marker = circle,
                nodeInputCallback = { onCircleClick },
                inputHandlerCallback = { onCircleClick }
            )
        }
        map.setOnGroundOverlayClickListener { groundOverlay ->
            decorations.findInputCallback<GroundOverlayNode, GroundOverlay, Unit>(
                nodeMatchPredicate = { it.groundOverlay == groundOverlay },
                nodeInputCallback = { onGroundOverlayClick },
                marker = groundOverlay,
                inputHandlerCallback = { onGroundOverlayClick }
            )
        }
        map.setOnPolygonClickListener { polygon ->
            decorations.findInputCallback<PolygonNode, Polygon, Unit>(
                nodeMatchPredicate = { it.polygon == polygon },
                nodeInputCallback = { onPolygonClick },
                marker = polygon,
                inputHandlerCallback = { onPolygonClick }
            )
        }
        map.setOnPolylineClickListener { polyline ->
            decorations.findInputCallback<PolylineNode, Polyline, Unit>(
                nodeMatchPredicate = { it.polyline == polyline },
                nodeInputCallback = { onPolylineClick },
                marker = polyline,
                inputHandlerCallback = { onPolylineClick }
            )
        }

        // Marker
        map.setOnMarkerClickListener { marker ->
            decorations.findInputCallback<MarkerNode, Marker, Boolean>(
                nodeMatchPredicate = { it.marker == marker },
                marker = marker,
                nodeInputCallback = { onMarkerClick },
                inputHandlerCallback = { onMarkerClick }
            )
        }
        map.setOnInfoWindowClickListener { marker ->
            decorations.findInputCallback<MarkerNode, Marker, Unit>(
                nodeMatchPredicate = { it.marker == marker },
                marker = marker,
                nodeInputCallback = { onInfoWindowClick },
                inputHandlerCallback = { onInfoWindowClick }
            )
        }
        map.setOnInfoWindowCloseListener { marker ->
            decorations.findInputCallback<MarkerNode, Marker, Unit>(
                nodeMatchPredicate = { it.marker == marker },
                marker = marker,
                nodeInputCallback = { onInfoWindowClose },
                inputHandlerCallback = { onInfoWindowClose }
            )
        }
        map.setOnInfoWindowLongClickListener { marker ->
            decorations.findInputCallback<MarkerNode, Marker, Unit>(
                nodeMatchPredicate = { it.marker == marker },
                marker = marker,
                nodeInputCallback = { onInfoWindowLongClick },
                inputHandlerCallback = { onInfoWindowLongClick }
            )
        }
        map.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDrag(marker: Marker) {
                decorations.findInputCallback<MarkerNode, Marker, Unit>(
                    nodeMatchPredicate = { it.marker == marker },
                    nodeInputCallback = {
                        {
                            markerState.position = it.position
                            markerState.dragState = DragState.DRAG
                        }
                    },
                    marker = marker,
                    inputHandlerCallback = { onMarkerDrag }
                )
            }

            override fun onMarkerDragEnd(marker: Marker) {
                decorations.findInputCallback<MarkerNode, Marker, Unit>(
                    nodeMatchPredicate = { it.marker == marker },
                    marker = marker,
                    nodeInputCallback = {
                        {
                            markerState.position = it.position
                            markerState.dragState = DragState.END
                        }
                    },
                    inputHandlerCallback = { onMarkerDragEnd }
                )
            }

            override fun onMarkerDragStart(marker: Marker) {
                decorations.findInputCallback<MarkerNode, Marker, Unit>(
                    nodeMatchPredicate = { it.marker == marker },
                    marker = marker,
                    nodeInputCallback = {
                        {
                            markerState.position = it.position
                            markerState.dragState = DragState.START
                        }
                    },
                    inputHandlerCallback = { onMarkerDragStart }
                )
            }
        })
        map.setInfoWindowAdapter(
            ComposeInfoWindowAdapter(
                mapView,
                markerNodeFinder = { marker ->
                    decorations.firstOrNull { it is MarkerNode && it.marker == marker }
                            as MarkerNode?
                }
            )
        )
    }
}

/**
 * General pattern for handling input. This finds the node that belongs to the clicked item, and executes the callback.
 *
 * If there is none, don't handle.
 */
private inline fun <reified NodeT : MapNode, I, O> Iterable<MapNode>.findInputCallback(
    nodeMatchPredicate: (NodeT) -> Boolean,
    nodeInputCallback: NodeT.() -> ((I) -> O)?,
    marker : I,
    inputHandlerCallback: InputHandlerNode.() -> ((I) -> O)?,
): Boolean {
    var callback: ((I) -> O)?
    for (item in this) {
        if (item is NodeT && nodeMatchPredicate(item)) {
            // Found a matching node
            if (nodeInputCallback(item)?.invoke(marker) == true) {
                return true
            }
        } else if (item is InputHandlerNode) {
            // Found an input handler, but keep looking for matching nodes
            callback = inputHandlerCallback(item)
            if (callback?.invoke(marker) == true) {
                return true
            }
        }
    }
    return false
}
