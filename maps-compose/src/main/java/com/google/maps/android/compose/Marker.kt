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

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.google.android.gms.maps.model.AdvancedMarkerOptions
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.PinConfig
import com.google.maps.android.ktx.addMarker

internal class MarkerNode(
    val compositionContext: CompositionContext,
    val marker: Marker,
    val markerState: MarkerState,
    var onMarkerClick: (Marker) -> Boolean,
    var onInfoWindowClick: (Marker) -> Unit,
    var onInfoWindowClose: (Marker) -> Unit,
    var onInfoWindowLongClick: (Marker) -> Unit,
    var infoWindow: (@Composable (Marker) -> Unit)?,
    var infoContent: (@Composable (Marker) -> Unit)?,
) : MapNode {
    override fun onAttached() {
        markerState.marker = marker
    }

    override fun onRemoved() {
        markerState.marker = null
        marker.remove()
    }

    override fun onCleared() {
        markerState.marker = null
        marker.remove()
    }
}

@Immutable
@Deprecated("START, DRAG, END are events, not states. Avoid usage.")
public enum class DragState {
    START, DRAG, END
}

/**
 * A state object that can be hoisted to control and observe the marker state.
 *
 * This cannot be used to preserve marker info window visibility across configuration changes.
 *
 * @param position the initial marker position
 */
public class MarkerState(
    position: LatLng = LatLng(0.0, 0.0),
) {
    /**
     * Current position of the marker.
     *
     * This property is backed by Compose state.
     * It can be updated by the API user and by the API itself:
     * it has two potentially competing sources of truth.
     *
     * The API will not update the property unless [isDragging] `== true`,
     * which will happen if and only if a Marker is draggable and the end user
     * is currently dragging it.
     */
    public var position: LatLng by mutableStateOf(position)

    /**
     * Reflects whether the end user is currently dragging the marker.
     * Dragging can happen only if a Marker is draggable.
     *
     * This property is backed by Compose state.
     */
    public var isDragging: Boolean by mutableStateOf(false)
        internal set

    /**
     * Current [DragState] of the marker.
     */
    @Deprecated(
        "Use isDragging instead - dragState is not appropriate for representing \"state\";" +
                " it is a lossy representation of drag \"events\", promoting invalid usage.",
        level = DeprecationLevel.WARNING
    )
    @Suppress("DEPRECATION")
    public var dragState: DragState by mutableStateOf(DragState.END)
        internal set

    // The marker associated with this MarkerState.
    private val markerState: MutableState<Marker?> = mutableStateOf(null)
    internal var marker: Marker?
        get() = markerState.value
        set(value) {
            if (markerState.value == null && value == null) return
            if (markerState.value != null && value != null) {
                error("MarkerState may only be associated with one Marker at a time.")
            }
            markerState.value = value
        }

    /**
     * Shows the info window for the underlying marker.
     *
     * Not backed by Compose state to accommodate
     * [com.google.android.gms.maps.GoogleMap] special semantics:
     * only a single info window can be visible for the entire GoogleMap.
     *
     * Only use from Compose Effect APIs, never directly from composition, to avoid exceptions and
     * unexpected behavior from cancelled compositions.
     */
    public fun showInfoWindow() {
        marker?.showInfoWindow()
    }

    /**
     * Hides the info window for the underlying marker.
     *
     * Not backed by observable Compose state to accommodate
     * [com.google.android.gms.maps.GoogleMap] special semantics:
     * only a single info window can be visible for the entire GoogleMap.
     *
     * Only use from Compose Effect APIs, never directly from composition, to avoid
     * unexpected behavior from cancelled compositions.
     */
    public fun hideInfoWindow() {
        marker?.hideInfoWindow()
    }

    public companion object {
        /**
         * The default saver implementation for [MarkerState]
         *
         * This cannot be used to preserve marker info window visibility across
         * configuration changes.
         */
        public val Saver: Saver<MarkerState, LatLng> = Saver(
            save = { it.position },
            restore = { MarkerState(it) }
        )
    }
}

/**
 * Uses [rememberSaveable] to retain [MarkerState.position] across configuration changes,
 * for simple use cases.
 *
 * Other use cases may be better served syncing [MarkerState.position] with a data model.
 *
 * This cannot be used to preserve info window visibility across configuration changes.
 */
@Composable
public fun rememberMarkerState(
    key: String? = null,
    position: LatLng = LatLng(0.0, 0.0)
): MarkerState = rememberSaveable(key = key, saver = MarkerState.Saver) {
    MarkerState(position)
}

/**
 * A composable for a marker on the map.
 * @param state the [MarkerState] to be used to control or observe the marker
 * state such as its position and info window
 * @param contentDescription the content description for accessibility purposes
 * @param alpha the alpha (opacity) of the marker
 * @param anchor the anchor for the marker image
 * @param draggable sets the draggability for the marker
 * @param flat sets if the marker should be flat against the map
 * @param icon sets the icon for the marker
 * @param infoWindowAnchor the anchor point of the info window on the marker image
 * @param rotation the rotation of the marker in degrees clockwise about the marker's anchor point
 * @param snippet the snippet for the marker
 * @param tag optional tag to associate with the marker
 * @param title the title for the marker
 * @param visible the visibility of the marker
 * @param zIndex the z-index of the marker
 * @param onClick a lambda invoked when the marker is clicked
 * @param onInfoWindowClick a lambda invoked when the marker's info window is clicked
 * @param onInfoWindowClose a lambda invoked when the marker's info window is closed
 * @param onInfoWindowLongClick a lambda invoked when the marker's info window is long clicked
 */
@Composable
@GoogleMapComposable
public fun Marker(
    state: MarkerState = rememberMarkerState(),
    contentDescription: String? = "",
    alpha: Float = 1.0f,
    anchor: Offset = Offset(0.5f, 1.0f),
    draggable: Boolean = false,
    flat: Boolean = false,
    icon: BitmapDescriptor? = null,
    infoWindowAnchor: Offset = Offset(0.5f, 0.0f),
    rotation: Float = 0.0f,
    snippet: String? = null,
    tag: Any? = null,
    title: String? = null,
    visible: Boolean = true,
    zIndex: Float = 0.0f,
    onClick: (Marker) -> Boolean = { false },
    onInfoWindowClick: (Marker) -> Unit = {},
    onInfoWindowClose: (Marker) -> Unit = {},
    onInfoWindowLongClick: (Marker) -> Unit = {},
) {
    MarkerImpl(
        state = state,
        contentDescription = contentDescription,
        alpha = alpha,
        anchor = anchor,
        draggable = draggable,
        flat = flat,
        icon = icon,
        infoWindowAnchor = infoWindowAnchor,
        rotation = rotation,
        snippet = snippet,
        tag = tag,
        title = title,
        visible = visible,
        zIndex = zIndex,
        onClick = onClick,
        onInfoWindowClick = onInfoWindowClick,
        onInfoWindowClose = onInfoWindowClose,
        onInfoWindowLongClick = onInfoWindowLongClick,
    )
}

/**
 * Composable rendering the content passed as a marker.
 *
 * @param keys unique keys representing the state of this Marker. Any changes to one of the key will
 * trigger a rendering of the content composable and thus the rendering of an updated marker.
 * @param state the [MarkerState] to be used to control or observe the marker
 * state such as its position and info window
 * @param contentDescription the content description for accessibility purposes
 * @param alpha the alpha (opacity) of the marker
 * @param anchor the anchor for the marker image
 * @param draggable sets the draggability for the marker
 * @param flat sets if the marker should be flat against the map
 * @param infoWindowAnchor the anchor point of the info window on the marker image
 * @param rotation the rotation of the marker in degrees clockwise about the marker's anchor point
 * @param snippet the snippet for the marker
 * @param tag optional tag to associate with the marker
 * @param title the title for the marker
 * @param visible the visibility of the marker
 * @param zIndex the z-index of the marker
 * @param onClick a lambda invoked when the marker is clicked
 * @param onInfoWindowClick a lambda invoked when the marker's info window is clicked
 * @param onInfoWindowClose a lambda invoked when the marker's info window is closed
 * @param onInfoWindowLongClick a lambda invoked when the marker's info window is long clicked
 * @param content composable lambda expression used to customize the marker's content
 */
@Composable
@GoogleMapComposable
public fun MarkerComposable(
    vararg keys: Any,
    state: MarkerState = rememberMarkerState(),
    contentDescription: String? = "",
    alpha: Float = 1.0f,
    anchor: Offset = Offset(0.5f, 1.0f),
    draggable: Boolean = false,
    flat: Boolean = false,
    infoWindowAnchor: Offset = Offset(0.5f, 0.0f),
    rotation: Float = 0.0f,
    snippet: String? = null,
    tag: Any? = null,
    title: String? = null,
    visible: Boolean = true,
    zIndex: Float = 0.0f,
    onClick: (Marker) -> Boolean = { false },
    onInfoWindowClick: (Marker) -> Unit = {},
    onInfoWindowClose: (Marker) -> Unit = {},
    onInfoWindowLongClick: (Marker) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val icon = rememberComposeBitmapDescriptor(*keys) { content() }

    MarkerImpl(
        state = state,
        contentDescription = contentDescription,
        alpha = alpha,
        anchor = anchor,
        draggable = draggable,
        flat = flat,
        icon = icon,
        infoWindowAnchor = infoWindowAnchor,
        rotation = rotation,
        snippet = snippet,
        tag = tag,
        title = title,
        visible = visible,
        zIndex = zIndex,
        onClick = onClick,
        onInfoWindowClick = onInfoWindowClick,
        onInfoWindowClose = onInfoWindowClose,
        onInfoWindowLongClick = onInfoWindowLongClick,
    )
}

/**
 * A composable for a marker on the map wherein its entire info window can be
 * customized. If this customization is not required, use
 * [com.google.maps.android.compose.Marker].
 *
 * @param state the [MarkerState] to be used to control or observe the marker
 * state such as its position and info window
 * @param alpha the alpha (opacity) of the marker
 * @param anchor the anchor for the marker image
 * @param draggable sets the draggability for the marker
 * @param flat sets if the marker should be flat against the map
 * @param icon sets the icon for the marker
 * @param infoWindowAnchor the anchor point of the info window on the marker image
 * @param rotation the rotation of the marker in degrees clockwise about the marker's anchor point
 * @param snippet the snippet for the marker
 * @param tag optional tag to associate with the marker
 * @param title the title for the marker
 * @param visible the visibility of the marker
 * @param zIndex the z-index of the marker
 * @param onClick a lambda invoked when the marker is clicked
 * @param onInfoWindowClick a lambda invoked when the marker's info window is clicked
 * @param onInfoWindowClose a lambda invoked when the marker's info window is closed
 * @param onInfoWindowLongClick a lambda invoked when the marker's info window is long clicked
 * @param content optional composable lambda expression for customizing the
 * info window's content
 */
@Composable
@GoogleMapComposable
public fun MarkerInfoWindow(
    state: MarkerState = rememberMarkerState(),
    alpha: Float = 1.0f,
    anchor: Offset = Offset(0.5f, 1.0f),
    draggable: Boolean = false,
    flat: Boolean = false,
    icon: BitmapDescriptor? = null,
    infoWindowAnchor: Offset = Offset(0.5f, 0.0f),
    rotation: Float = 0.0f,
    snippet: String? = null,
    tag: Any? = null,
    title: String? = null,
    visible: Boolean = true,
    zIndex: Float = 0.0f,
    onClick: (Marker) -> Boolean = { false },
    onInfoWindowClick: (Marker) -> Unit = {},
    onInfoWindowClose: (Marker) -> Unit = {},
    onInfoWindowLongClick: (Marker) -> Unit = {},
    content: (@Composable (Marker) -> Unit)? = null
) {
    MarkerImpl(
        state = state,
        alpha = alpha,
        anchor = anchor,
        draggable = draggable,
        flat = flat,
        icon = icon,
        infoWindowAnchor = infoWindowAnchor,
        rotation = rotation,
        snippet = snippet,
        tag = tag,
        title = title,
        visible = visible,
        zIndex = zIndex,
        onClick = onClick,
        onInfoWindowClick = onInfoWindowClick,
        onInfoWindowClose = onInfoWindowClose,
        onInfoWindowLongClick = onInfoWindowLongClick,
        infoWindow = content,
    )
}

/**
 * A composable for a marker on the map wherein its info window contents can be
 * customized. If this customization is not required, use
 * [com.google.maps.android.compose.Marker].
 *
 * @param state the [MarkerState] to be used to control or observe the marker
 * state such as its position and info window
 * @param alpha the alpha (opacity) of the marker
 * @param anchor the anchor for the marker image
 * @param draggable sets the draggability for the marker
 * @param flat sets if the marker should be flat against the map
 * @param icon sets the icon for the marker
 * @param infoWindowAnchor the anchor point of the info window on the marker image
 * @param rotation the rotation of the marker in degrees clockwise about the marker's anchor point
 * @param snippet the snippet for the marker
 * @param tag optional tag to associate with the marker
 * @param title the title for the marker
 * @param visible the visibility of the marker
 * @param zIndex the z-index of the marker
 * @param onClick a lambda invoked when the marker is clicked
 * @param onInfoWindowClick a lambda invoked when the marker's info window is clicked
 * @param onInfoWindowClose a lambda invoked when the marker's info window is closed
 * @param onInfoWindowLongClick a lambda invoked when the marker's info window is long clicked
 * @param content optional composable lambda expression for customizing the
 * info window's content
 */
@Composable
@GoogleMapComposable
public fun MarkerInfoWindowContent(
    state: MarkerState = rememberMarkerState(),
    alpha: Float = 1.0f,
    anchor: Offset = Offset(0.5f, 1.0f),
    draggable: Boolean = false,
    flat: Boolean = false,
    icon: BitmapDescriptor? = null,
    infoWindowAnchor: Offset = Offset(0.5f, 0.0f),
    rotation: Float = 0.0f,
    snippet: String? = null,
    tag: Any? = null,
    title: String? = null,
    visible: Boolean = true,
    zIndex: Float = 0.0f,
    onClick: (Marker) -> Boolean = { false },
    onInfoWindowClick: (Marker) -> Unit = {},
    onInfoWindowClose: (Marker) -> Unit = {},
    onInfoWindowLongClick: (Marker) -> Unit = {},
    content: (@Composable (Marker) -> Unit)? = null
) {
    MarkerImpl(
        state = state,
        alpha = alpha,
        anchor = anchor,
        draggable = draggable,
        flat = flat,
        icon = icon,
        infoWindowAnchor = infoWindowAnchor,
        rotation = rotation,
        snippet = snippet,
        tag = tag,
        title = title,
        visible = visible,
        zIndex = zIndex,
        onClick = onClick,
        onInfoWindowClick = onInfoWindowClick,
        onInfoWindowClose = onInfoWindowClose,
        onInfoWindowLongClick = onInfoWindowLongClick,
        infoContent = content,
    )
}

/**
 * Internal implementation for a marker on a Google map.
 *
 * @param state the [MarkerState] to be used to control or observe the marker
 * state such as its position and info window
 * @param contentDescription the content description for accessibility purposes
 * @param alpha the alpha (opacity) of the marker
 * @param anchor the anchor for the marker image
 * @param draggable sets the draggability for the marker
 * @param flat sets if the marker should be flat against the map
 * @param icon sets the icon for the marker
 * @param infoWindowAnchor the anchor point of the info window on the marker image
 * @param rotation the rotation of the marker in degrees clockwise about the marker's anchor point
 * @param snippet the snippet for the marker
 * @param tag optional tag to associate with the marker
 * @param title the title for the marker
 * @param visible the visibility of the marker
 * @param zIndex the z-index of the marker
 * @param onClick a lambda invoked when the marker is clicked
 * @param onInfoWindowClick a lambda invoked when the marker's info window is clicked
 * @param onInfoWindowClose a lambda invoked when the marker's info window is closed
 * @param onInfoWindowLongClick a lambda invoked when the marker's info window is long clicked
 * @param infoWindow optional composable lambda expression for customizing
 * the entire info window. If this value is non-null, the value in infoContent]
 * will be ignored.
 * @param infoContent optional composable lambda expression for customizing
 * the info window's content. If this value is non-null, [infoWindow] must be null.
 */
@Composable
@GoogleMapComposable
private fun MarkerImpl(
    state: MarkerState = rememberMarkerState(),
    contentDescription: String? = "",
    alpha: Float = 1.0f,
    anchor: Offset = Offset(0.5f, 1.0f),
    draggable: Boolean = false,
    flat: Boolean = false,
    icon: BitmapDescriptor? = null,
    infoWindowAnchor: Offset = Offset(0.5f, 0.0f),
    rotation: Float = 0.0f,
    snippet: String? = null,
    tag: Any? = null,
    title: String? = null,
    visible: Boolean = true,
    zIndex: Float = 0.0f,
    onClick: (Marker) -> Boolean = { false },
    onInfoWindowClick: (Marker) -> Unit = {},
    onInfoWindowClose: (Marker) -> Unit = {},
    onInfoWindowLongClick: (Marker) -> Unit = {},
    infoWindow: (@Composable (Marker) -> Unit)? = null,
    infoContent: (@Composable (Marker) -> Unit)? = null,
) {
    val mapApplier = currentComposer.applier as? MapApplier
    val compositionContext = rememberCompositionContext()
    ComposeNode<MarkerNode, MapApplier>(
        factory = {
            val marker = mapApplier?.map?.addMarker {
                contentDescription(contentDescription)
                alpha(alpha)
                anchor(anchor.x, anchor.y)
                draggable(draggable)
                flat(flat)
                icon(icon)
                infoWindowAnchor(infoWindowAnchor.x, infoWindowAnchor.y)
                position(state.position)
                rotation(rotation)
                snippet(snippet)
                title(title)
                visible(visible)
                zIndex(zIndex)
            } ?: error("Error adding marker")
            marker.tag = tag
            MarkerNode(
                compositionContext = compositionContext,
                marker = marker,
                markerState = state,
                onMarkerClick = onClick,
                onInfoWindowClick = onInfoWindowClick,
                onInfoWindowClose = onInfoWindowClose,
                onInfoWindowLongClick = onInfoWindowLongClick,
                infoContent = infoContent,
                infoWindow = infoWindow,
            )
        },
        update = {
            update(onClick) { this.onMarkerClick = it }
            update(onInfoWindowClick) { this.onInfoWindowClick = it }
            update(onInfoWindowClose) { this.onInfoWindowClose = it }
            update(onInfoWindowLongClick) { this.onInfoWindowLongClick = it }
            update(infoContent) { this.infoContent = it }
            update(infoWindow) { this.infoWindow = it }

            update(alpha) { this.marker.alpha = it }
            update(anchor) { this.marker.setAnchor(it.x, it.y) }
            update(draggable) { this.marker.isDraggable = it }
            update(flat) { this.marker.isFlat = it }
            update(icon) { this.marker.setIcon(it) }
            update(infoWindowAnchor) { this.marker.setInfoWindowAnchor(it.x, it.y) }
            update(state.position) { this.marker.position = it }
            update(rotation) { this.marker.rotation = it }
            update(snippet) {
                this.marker.snippet = it
                if (this.marker.isInfoWindowShown) {
                    this.marker.showInfoWindow()
                }
            }
            update(tag) { this.marker.tag = it }
            update(title) {
                this.marker.title = it
                if (this.marker.isInfoWindowShown) {
                    this.marker.showInfoWindow()
                }
            }
            update(visible) { this.marker.isVisible = it }
            update(zIndex) { this.marker.zIndex = it }
        }
    )
}


/**
 * A composable for an advanced marker on the map.
 *
 * @param state the [MarkerState] to be used to control or observe the marker
 * state such as its position and info window
 * @param contentDescription the content description for accessibility purposes
 * @param alpha the alpha (opacity) of the marker
 * @param anchor the anchor for the marker image
 * @param draggable sets the draggability for the marker
 * @param flat sets if the marker should be flat against the map
 * @param infoWindowAnchor the anchor point of the info window on the marker image
 * @param rotation the rotation of the marker in degrees clockwise about the marker's anchor point
 * @param snippet the snippet for the marker
 * @param tag optional tag to associate with the marker
 * @param title the title for the marker
 * @param visible the visibility of the marker
 * @param zIndex the z-index of the marker
 * @param onClick a lambda invoked when the marker is clicked
 * @param onInfoWindowClick a lambda invoked when the marker's info window is clicked
 * @param onInfoWindowClose a lambda invoked when the marker's info window is closed
 * @param onInfoWindowLongClick a lambda invoked when the marker's info window is long clicked
 * @param pinConfig the PinConfig object that will be used for the advanced marker
 * @param iconView the custom view to be used on the advanced marker
 * @param collisionBehavior the expected collision behavior
 */
@Composable
@GoogleMapComposable
public fun AdvancedMarker(
    state: MarkerState = rememberMarkerState(),
    contentDescription: String? = "",
    alpha: Float = 1.0f,
    anchor: Offset = Offset(0.5f, 1.0f),
    draggable: Boolean = false,
    flat: Boolean = false,
    infoWindowAnchor: Offset = Offset(0.5f, 0.0f),
    rotation: Float = 0.0f,
    snippet: String? = null,
    tag: Any? = null,
    title: String? = null,
    visible: Boolean = true,
    zIndex: Float = 0.0f,
    onClick: (Marker) -> Boolean = { false },
    onInfoWindowClick: (Marker) -> Unit = {},
    onInfoWindowClose: (Marker) -> Unit = {},
    onInfoWindowLongClick: (Marker) -> Unit = {},
    pinConfig: PinConfig? = null,
    iconView: View? = null,
    collisionBehavior: Int = AdvancedMarkerOptions.CollisionBehavior.REQUIRED
) {
    AdvancedMarkerImpl(
        state = state,
        contentDescription = contentDescription,
        alpha = alpha,
        anchor = anchor,
        draggable = draggable,
        flat = flat,
        infoWindowAnchor = infoWindowAnchor,
        rotation = rotation,
        snippet = snippet,
        tag = tag,
        title = title,
        visible = visible,
        zIndex = zIndex,
        onClick = onClick,
        onInfoWindowClick = onInfoWindowClick,
        onInfoWindowClose = onInfoWindowClose,
        onInfoWindowLongClick = onInfoWindowLongClick,
        pinConfig = pinConfig,
        iconView = iconView,
        collisionBehavior = collisionBehavior
    )
}

/**
 * Internal implementation for an advanced marker on a Google map.
 *
 * @param state the [MarkerState] to be used to control or observe the marker
 * state such as its position and info window
 * @param contentDescription the content description for accessibility purposes
 * @param alpha the alpha (opacity) of the marker
 * @param anchor the anchor for the marker image
 * @param draggable sets the draggability for the marker
 * @param flat sets if the marker should be flat against the map
 * @param infoWindowAnchor the anchor point of the info window on the marker image
 * @param rotation the rotation of the marker in degrees clockwise about the marker's anchor point
 * @param snippet the snippet for the marker
 * @param tag optional tag to associate with the marker
 * @param title the title for the marker
 * @param visible the visibility of the marker
 * @param zIndex the z-index of the marker
 * @param onClick a lambda invoked when the marker is clicked
 * @param onInfoWindowClick a lambda invoked when the marker's info window is clicked
 * @param onInfoWindowClose a lambda invoked when the marker's info window is closed
 * @param onInfoWindowLongClick a lambda invoked when the marker's info window is long clicked
 * @param infoWindow optional composable lambda expression for customizing
 * the entire info window. If this value is non-null, the value in infoContent]
 * will be ignored.
 * @param infoContent optional composable lambda expression for customizing
 * the info window's content. If this value is non-null, [infoWindow] must be null.
 * @param pinConfig the PinConfig object that will be used for the advanced marker
 * @param iconView the custom view to be used on the advanced marker
 * @param collisionBehavior the expected collision behavior
 */
@Composable
@GoogleMapComposable
private fun AdvancedMarkerImpl(
    state: MarkerState = rememberMarkerState(),
    contentDescription: String? = "",
    alpha: Float = 1.0f,
    anchor: Offset = Offset(0.5f, 1.0f),
    draggable: Boolean = false,
    flat: Boolean = false,
    infoWindowAnchor: Offset = Offset(0.5f, 0.0f),
    rotation: Float = 0.0f,
    snippet: String? = null,
    tag: Any? = null,
    title: String? = null,
    visible: Boolean = true,
    zIndex: Float = 0.0f,
    onClick: (Marker) -> Boolean = { false },
    onInfoWindowClick: (Marker) -> Unit = {},
    onInfoWindowClose: (Marker) -> Unit = {},
    onInfoWindowLongClick: (Marker) -> Unit = {},
    infoWindow: (@Composable (Marker) -> Unit)? = null,
    infoContent: (@Composable (Marker) -> Unit)? = null,
    pinConfig: PinConfig? = null,
    iconView: View? = null,
    collisionBehavior: Int = AdvancedMarkerOptions.CollisionBehavior.REQUIRED
) {

    val mapApplier = currentComposer.applier as? MapApplier
    val compositionContext = rememberCompositionContext()

    ComposeNode<MarkerNode, MapApplier>(
        factory = {
            val advancedMarkerOptions = AdvancedMarkerOptions()
                .position(state.position)
                .collisionBehavior(collisionBehavior)
            if (iconView != null) {
                advancedMarkerOptions.iconView(iconView)
            } else if (pinConfig != null) {
                advancedMarkerOptions.icon(BitmapDescriptorFactory.fromPinConfig(pinConfig))
            }
            advancedMarkerOptions.contentDescription(contentDescription)
            advancedMarkerOptions.alpha(alpha)
            advancedMarkerOptions.anchor(anchor.x, anchor.y)
            advancedMarkerOptions.draggable(draggable)
            advancedMarkerOptions.flat(flat)
            advancedMarkerOptions.infoWindowAnchor(infoWindowAnchor.x, infoWindowAnchor.y)
            advancedMarkerOptions.position(state.position)
            advancedMarkerOptions.rotation(rotation)
            advancedMarkerOptions.snippet(snippet)
            advancedMarkerOptions.title(title)
            advancedMarkerOptions.visible(visible)
            advancedMarkerOptions.zIndex(zIndex)
            val marker = mapApplier?.map?.addMarker(advancedMarkerOptions)
                ?: error("Error adding marker")
            marker.tag = tag
            MarkerNode(
                compositionContext = compositionContext,
                marker = marker,
                markerState = state,
                onMarkerClick = onClick,
                onInfoWindowClick = onInfoWindowClick,
                onInfoWindowClose = onInfoWindowClose,
                onInfoWindowLongClick = onInfoWindowLongClick,
                infoContent = infoContent,
                infoWindow = infoWindow,
            )
        },
        update = {
            update(onClick) { this.onMarkerClick = it }
            update(onInfoWindowClick) { this.onInfoWindowClick = it }
            update(onInfoWindowClose) { this.onInfoWindowClose = it }
            update(onInfoWindowLongClick) { this.onInfoWindowLongClick = it }
            update(infoContent) { this.infoContent = it }
            update(infoWindow) { this.infoWindow = it }

            update(alpha) { this.marker.alpha = it }
            update(anchor) { this.marker.setAnchor(it.x, it.y) }
            update(draggable) { this.marker.isDraggable = it }
            update(flat) { this.marker.isFlat = it }
            update(infoWindowAnchor) { this.marker.setInfoWindowAnchor(it.x, it.y) }
            update(state.position) { this.marker.position = it }
            update(rotation) { this.marker.rotation = it }
            update(snippet) {
                this.marker.snippet = it
                if (this.marker.isInfoWindowShown) {
                    this.marker.showInfoWindow()
                }
            }
            update(tag) { this.marker.tag = it }
            update(title) {
                this.marker.title = it
                if (this.marker.isInfoWindowShown) {
                    this.marker.showInfoWindow()
                }
            }
            update(pinConfig) {
                if (iconView == null) {
                    this.marker.setIcon(pinConfig?.let { it1 ->
                        BitmapDescriptorFactory.fromPinConfig(
                            it1
                        )
                    })
                }
            }

            update(visible) { this.marker.isVisible = it }
            update(zIndex) { this.marker.zIndex = it }
        }
    )
}
