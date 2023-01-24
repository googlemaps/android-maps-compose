package com.google.maps.android.compose

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.GroundOverlay
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.model.Polyline

/**
 * A generic handler for map input.
 * Non-null lambdas will be invoked if no other node was able to handle that input.
 * For example, if [OnMarkerClickListener.onMarkerClick] was invoked and no matching [MarkerNode]
 * was found, this [onMarkerClick] will be invoked.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun InputHandler(
    onCircleClick: ((Circle) -> Unit)? = null,
    onGroundOverlayClick: ((GroundOverlay) -> Unit)? = null,
    onPolygonClick: ((Polygon) -> Unit)? = null,
    onPolylineClick: ((Polyline) -> Unit)? = null,
    onMarkerClick: ((Marker) -> Boolean)? = null,
    onInfoWindowClick: ((Marker) -> Unit)? = null,
    onInfoWindowClose: ((Marker) -> Unit)? = null,
    onInfoWindowLongClick: ((Marker) -> Unit)? = null,
    onMarkerDrag: ((Marker) -> Unit)? = null,
    onMarkerDragEnd: ((Marker) -> Unit)? = null,
    onMarkerDragStart: ((Marker) -> Unit)? = null,
) {
    ComposeNode<InputHandlerNode, MapApplier>(
        factory = {
            InputHandlerNode(
                onCircleClick,
                onGroundOverlayClick,
                onPolygonClick,
                onPolylineClick,
                onMarkerClick,
                onInfoWindowClick,
                onInfoWindowClose,
                onInfoWindowLongClick,
                onMarkerDrag,
                onMarkerDragEnd,
                onMarkerDragStart,
            )
        },
        update = {
            update(onCircleClick) { this.onCircleClick = it }
            update(onGroundOverlayClick) { this.onGroundOverlayClick = it }
            update(onPolygonClick) { this.onPolygonClick = it }
            update(onPolylineClick) { this.onPolylineClick = it }
            update(onMarkerClick) { this.onMarkerClick = it }
            update(onInfoWindowClick) { this.onInfoWindowClick = it }
            update(onInfoWindowClose) { this.onInfoWindowClose = it }
            update(onInfoWindowLongClick) { this.onInfoWindowLongClick = it }
            update(onMarkerDrag) { this.onMarkerDrag = it }
            update(onMarkerDragEnd) { this.onMarkerDragEnd = it }
            update(onMarkerDragStart) { this.onMarkerDragStart = it }
        }
    )
}

internal class InputHandlerNode(
    onCircleClick: ((Circle) -> Unit)? = null,
    onGroundOverlayClick: ((GroundOverlay) -> Unit)? = null,
    onPolygonClick: ((Polygon) -> Unit)? = null,
    onPolylineClick: ((Polyline) -> Unit)? = null,
    onMarkerClick: ((Marker) -> Boolean)? = null,
    onInfoWindowClick: ((Marker) -> Unit)? = null,
    onInfoWindowClose: ((Marker) -> Unit)? = null,
    onInfoWindowLongClick: ((Marker) -> Unit)? = null,
    onMarkerDrag: ((Marker) -> Unit)? = null,
    onMarkerDragEnd: ((Marker) -> Unit)? = null,
    onMarkerDragStart: ((Marker) -> Unit)? = null,
) : MapNode {
    var onCircleClick: ((Circle) -> Unit)? by mutableStateOf(onCircleClick)
    var onGroundOverlayClick: ((GroundOverlay) -> Unit)? by mutableStateOf(onGroundOverlayClick)
    var onPolygonClick: ((Polygon) -> Unit)? by mutableStateOf(onPolygonClick)
    var onPolylineClick: ((Polyline) -> Unit)? by mutableStateOf(onPolylineClick)
    var onMarkerClick: ((Marker) -> Boolean)? by mutableStateOf(onMarkerClick)
    var onInfoWindowClick: ((Marker) -> Unit)? by mutableStateOf(onInfoWindowClick)
    var onInfoWindowClose: ((Marker) -> Unit)? by mutableStateOf(onInfoWindowClose)
    var onInfoWindowLongClick: ((Marker) -> Unit)? by mutableStateOf(onInfoWindowLongClick)
    var onMarkerDrag: ((Marker) -> Unit)? by mutableStateOf(onMarkerDrag)
    var onMarkerDragEnd: ((Marker) -> Unit)? by mutableStateOf(onMarkerDragEnd)
    var onMarkerDragStart: ((Marker) -> Unit)? by mutableStateOf(onMarkerDragStart)
}
