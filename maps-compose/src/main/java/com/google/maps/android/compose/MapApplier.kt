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
    internal val mapView: MapView,
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
                nodeInputCallback = { onCircleClick },
                inputHandlerCallback = { onCircleClick }
            )?.invoke(circle)
        }
        map.setOnGroundOverlayClickListener { groundOverlay ->
            decorations.findInputCallback<GroundOverlayNode, GroundOverlay, Unit>(
                nodeMatchPredicate = { it.groundOverlay == groundOverlay },
                nodeInputCallback = { onGroundOverlayClick },
                inputHandlerCallback = { onGroundOverlayClick }
            )?.invoke(groundOverlay)
        }
        map.setOnPolygonClickListener { polygon ->
            decorations.findInputCallback<PolygonNode, Polygon, Unit>(
                nodeMatchPredicate = { it.polygon == polygon },
                nodeInputCallback = { onPolygonClick },
                inputHandlerCallback = { onPolygonClick }
            )?.invoke(polygon)
        }
        map.setOnPolylineClickListener { polyline ->
            decorations.findInputCallback<PolylineNode, Polyline, Unit>(
                nodeMatchPredicate = { it.polyline == polyline },
                nodeInputCallback = { onPolylineClick },
                inputHandlerCallback = { onPolylineClick }
            )?.invoke(polyline)
        }

        // Marker
        map.setOnMarkerClickListener { marker ->
            decorations.findInputCallback<MarkerNode, Marker, Boolean>(
                nodeMatchPredicate = { it.marker == marker },
                nodeInputCallback = { onMarkerClick },
                inputHandlerCallback = { onMarkerClick }
            )?.invoke(marker)
                ?: false
        }
        map.setOnInfoWindowClickListener { marker ->
            decorations.findInputCallback<MarkerNode, Marker, Unit>(
                nodeMatchPredicate = { it.marker == marker },
                nodeInputCallback = { onInfoWindowClick },
                inputHandlerCallback = { onInfoWindowClick }
            )?.invoke(marker)
        }
        map.setOnInfoWindowCloseListener { marker ->
            decorations.findInputCallback<MarkerNode, Marker, Unit>(
                nodeMatchPredicate = { it.marker == marker },
                nodeInputCallback = { onInfoWindowClose },
                inputHandlerCallback = { onInfoWindowClose }
            )?.invoke(marker)
        }
        map.setOnInfoWindowLongClickListener { marker ->
            decorations.findInputCallback<MarkerNode, Marker, Unit>(
                nodeMatchPredicate = { it.marker == marker },
                nodeInputCallback = { onInfoWindowLongClick },
                inputHandlerCallback = { onInfoWindowLongClick }
            )?.invoke(marker)
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
                    inputHandlerCallback = { onMarkerDrag }
                )?.invoke(marker)
            }

            override fun onMarkerDragEnd(marker: Marker) {
                decorations.findInputCallback<MarkerNode, Marker, Unit>(
                    nodeMatchPredicate = { it.marker == marker },
                    nodeInputCallback = {
                        {
                            markerState.position = it.position
                            markerState.dragState = DragState.END
                        }
                    },
                    inputHandlerCallback = { onMarkerDragEnd }
                )?.invoke(marker)
            }

            override fun onMarkerDragStart(marker: Marker) {
                decorations.findInputCallback<MarkerNode, Marker, Unit>(
                    nodeMatchPredicate = { it.marker == marker },
                    nodeInputCallback = {
                        {
                            markerState.position = it.position
                            markerState.dragState = DragState.START
                        }
                    },
                    inputHandlerCallback = { onMarkerDragStart }
                )?.invoke(marker)
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
 * General pattern for handling input:
 * Find the node that belongs to the clicked item.
 * If there is none, default to the first InputHandlerNode.
 * If there is none, don't handle.
 */
private inline fun <reified NodeT : MapNode, I, O> Iterable<MapNode>.findInputCallback(
    nodeMatchPredicate: (NodeT) -> Boolean,
    nodeInputCallback: NodeT.() -> ((I) -> O)?,
    inputHandlerCallback: InputHandlerNode.() -> ((I) -> O)?,
): ((I) -> O)? {
    var callback: ((I) -> O)? = null
    for (item in this) {
        if (item is NodeT && nodeMatchPredicate(item)) {
            // Found a matching node
            return nodeInputCallback(item)
        } else if (item is InputHandlerNode) {
            // Found an input handler, but keep looking for matching nodes
            callback = inputHandlerCallback(item)
        }
    }
    return callback
}
