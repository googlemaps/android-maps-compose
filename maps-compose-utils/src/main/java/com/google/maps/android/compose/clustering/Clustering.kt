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

package com.google.maps.android.compose.clustering

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.UiComposable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.ClusterRenderer
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.InfoWindowHandler
import com.google.maps.android.compose.InputHandler
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.currentCameraPositionState
import com.google.maps.android.compose.rememberComposeUiViewRenderer
import com.google.maps.android.compose.rememberReattachClickListenersHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import kotlin.coroutines.CoroutineContext

/**
 * Properties for a marker in [Clustering].
 */
public class ClusteringMarkerProperties {
    public var anchor: Offset? by mutableStateOf(null)
        internal set
    public var zIndex: Float? by mutableStateOf(null)
        internal set
    public var rotation: Float? by mutableStateOf(null)
        internal set
    public var contentDescription: String? by mutableStateOf(null)
        internal set
}

/**
 * [CompositionLocal] used to provide [ClusteringMarkerProperties] to the content of a cluster or
 * cluster item.
 */
public val LocalClusteringMarkerProperties:
    androidx.compose.runtime.ProvidableCompositionLocal<ClusteringMarkerProperties> =
    staticCompositionLocalOf { ClusteringMarkerProperties() }

/**
 * Helper function to specify properties for the marker representing a cluster or cluster item.
 *
 * @param anchor the anchor for the marker image. If null, the default anchor specified in
 * [Clustering] will be used.
 * @param zIndex the z-index of the marker. If null, the default z-index specified in [Clustering]
 * will be used.
 * @param rotation the rotation of the marker in degrees clockwise about the marker's anchor point.
 * If null, the default rotation specified in [Clustering] will be used.
 * @param contentDescription the content description for accessibility purposes. If null, the title
 * of the [ClusterItem] will be used as a fallback for cluster items.
 */
@Composable
public fun ClusteringMarkerProperties(
    anchor: Offset? = null,
    zIndex: Float? = null,
    rotation: Float? = null,
    contentDescription: String? = null,
) {
    val properties = LocalClusteringMarkerProperties.current
    SideEffect {
        properties.anchor = anchor
        properties.zIndex = zIndex
        properties.rotation = rotation
        properties.contentDescription = contentDescription
    }
}

/**
 * Helper function to specify properties for the marker representing a cluster or cluster item.
 *
 * Retained for binary compatibility with binaries compiled against earlier versions.
 */
@Deprecated(
    message = "Retained for binary compatibility.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
public fun ClusteringMarkerProperties(anchor: Offset? = null, zIndex: Float? = null, rotation: Float? = null) {
    ClusteringMarkerProperties(
        anchor = anchor,
        zIndex = zIndex,
        rotation = rotation,
        contentDescription = null,
    )
}

/**
 * Groups many items on a map based on zoom level.
 *
 * @param items all items to show
 * @param onClusterClick a lambda invoked when the user clicks a cluster of items
 * @param onClusterItemClick a lambda invoked when the user clicks a non-clustered item
 * @param onClusterItemInfoWindowClick a lambda invoked when the user clicks the info window of a
 * non-clustered item
 * @param onClusterItemInfoWindowLongClick a lambda invoked when the user long-clicks the info
 * window of a non-clustered item
 * @param clusterContent an optional Composable that is rendered for each [Cluster].
 * @param clusterItemContent an optional Composable that is rendered for each non-clustered item.
 * @param clusterContentAnchor the anchor for the cluster image
 * @param clusterItemContentAnchor the anchor for the non-clustered item image
 * @param clusterContentZIndex the z-index of the cluster
 * @param clusterItemContentZIndex the z-index of the non-clustered item
 * @param clusterContentRotation the rotation of the cluster in degrees clockwise about the marker's anchor point
 * @param clusterItemContentRotation the rotation of the non-clustered item in degrees clockwise about the marker's anchor point
 * @param clusterRenderer an optional ClusterRenderer that can be used to specify the algorithm used by the rendering.
 * @param key an optional stable, unique key for each item. When supplied, items with the same key
 * are treated as updates when their values differ. Duplicate keys in a collection produce a warning
 * and only the last item for each key is kept. Without a key, items are identified by their
 * `equals` and `hashCode` implementations.
 * @param clusterItemDecoration optional map content drawn for each item while it isn't part of a cluster
 */
@Composable
@GoogleMapComposable
@MapsComposeExperimentalApi
@Deprecated(
    message = "If clusterRenderer is specified, clusterContent and clusterItemContent are not used; " +
        "use a function that takes ClusterManager as an argument instead.",
    replaceWith = ReplaceWith(
        expression = """
            val clusterManager = rememberClusterManager<T>()
            LaunchedEffect(clusterManager, clusterRenderer) {
                clusterManager?.renderer = clusterRenderer
            }
            SideEffect {
                clusterManager ?: return@SideEffect
                clusterManager.setOnClusterClickListener(onClusterClick)
                clusterManager.setOnClusterItemClickListener(onClusterItemClick)
                clusterManager.setOnClusterItemInfoWindowClickListener(onClusterItemInfoWindowClick)
                clusterManager.setOnClusterItemInfoWindowLongClickListener(onClusterItemInfoWindowLongClick)
            }
            // Wait for renderer to apply before clustering
            if (clusterManager != null && clusterManager.renderer == clusterRenderer) {
                Clustering(
                    items = items,
                    clusterManager = clusterManager,
                )
            }
        """,
        imports = [
            "com.google.maps.android.compose.clustering.Clustering",
            "androidx.compose.runtime.SideEffect",
            "com.google.maps.android.clustering.ClusterManager",
        ],
    ),
)
public fun <T : ClusterItem> Clustering(
    items: Collection<T>,
    onClusterClick: (Cluster<T>) -> Boolean = { false },
    onClusterItemClick: (T) -> Boolean = { false },
    onClusterItemInfoWindowClick: (T) -> Unit = { },
    onClusterItemInfoWindowLongClick: (T) -> Unit = { },
    clusterContent: @[UiComposable Composable] ((Cluster<T>) -> Unit)? = null,
    clusterItemContent: @[UiComposable Composable] ((T) -> Unit)? = null,
    clusterContentAnchor: Offset = Offset(0.5f, 1.0f),
    clusterItemContentAnchor: Offset = Offset(0.5f, 1.0f),
    clusterContentZIndex: Float = 0.0f,
    clusterItemContentZIndex: Float = 0.0f,
    clusterContentRotation: Float = 0.0f,
    clusterItemContentRotation: Float = 0.0f,
    clusterRenderer: ClusterRenderer<T>? = null,
    key: ((T) -> Any)? = null,
    clusterItemDecoration:
    @Composable @GoogleMapComposable
    (T) -> Unit = {},
) {
    val clusterManager = rememberClusterManager(
        clusterContent,
        clusterItemContent,
        clusterContentAnchor,
        clusterItemContentAnchor,
        clusterContentZIndex,
        clusterItemContentZIndex,
        clusterContentRotation,
        clusterItemContentRotation,
        clusterRenderer,
    ) ?: return

    SideEffect {
        clusterManager.setOnClusterClickListener(onClusterClick)
        clusterManager.setOnClusterItemClickListener(onClusterItemClick)
        clusterManager.setOnClusterItemInfoWindowClickListener(onClusterItemInfoWindowClick)
        clusterManager.setOnClusterItemInfoWindowLongClickListener(onClusterItemInfoWindowLongClick)
    }
    Clustering(
        items = items,
        clusterManager = clusterManager,
        clusterItemDecoration = clusterItemDecoration,
        renderer = clusterManager.renderer,
        key = key,
    )
}

/**
 * Groups many items on a map based on zoom level.
 *
 * @param items all items to show
 * @param onClusterClick a lambda invoked when the user clicks a cluster of items
 * @param onClusterItemClick a lambda invoked when the user clicks a non-clustered item
 * @param onClusterItemInfoWindowClick a lambda invoked when the user clicks the info window of a
 * non-clustered item
 * @param onClusterItemInfoWindowLongClick a lambda invoked when the user long-clicks the info
 * window of a non-clustered item
 * @param clusterContent an optional Composable that is rendered for each [Cluster].
 * @param clusterItemContent an optional Composable that is rendered for each non-clustered item.
 * @param clusterContentAnchor the anchor for the cluster image
 * @param clusterItemContentAnchor the anchor for the non-clustered item image
 * @param clusterContentZIndex the z-index of the cluster
 * @param clusterItemContentZIndex the z-index of the non-clustered item
 * @param clusterContentRotation the rotation of the cluster in degrees clockwise about the marker's anchor point
 * @param clusterItemContentRotation the rotation of the non-clustered item in degrees clockwise about the marker's anchor point
 * @param key an optional stable, unique key for each item. When supplied, items with the same key
 * are treated as updates when their values differ. Without a key, items are identified by their
 * `equals` and `hashCode` implementations. Duplicate keys in a collection produce a warning
 * and only the last item for each key is kept.
 * @param clusterItemDecoration optional map content drawn for each item while it isn't part of a cluster
 */
@Composable
@GoogleMapComposable
@MapsComposeExperimentalApi
public fun <T : ClusterItem> Clustering(
    items: Collection<T>,
    onClusterClick: (Cluster<T>) -> Boolean = { false },
    onClusterItemClick: (T) -> Boolean = { false },
    onClusterItemInfoWindowClick: (T) -> Unit = { },
    onClusterItemInfoWindowLongClick: (T) -> Unit = { },
    clusterContent: @[UiComposable Composable] ((Cluster<T>) -> Unit)? = null,
    clusterItemContent: @[UiComposable Composable] ((T) -> Unit)? = null,
    clusterContentAnchor: Offset = Offset(0.5f, 1.0f),
    clusterItemContentAnchor: Offset = Offset(0.5f, 1.0f),
    clusterContentZIndex: Float = 0.0f,
    clusterItemContentZIndex: Float = 0.0f,
    clusterContentRotation: Float = 0.0f,
    clusterItemContentRotation: Float = 0.0f,
    key: ((T) -> Any)? = null,
    clusterItemDecoration:
    @Composable @GoogleMapComposable
    (T) -> Unit = {},
) {
    Clustering(
        items = items,
        onClusterClick = onClusterClick,
        onClusterItemClick = onClusterItemClick,
        onClusterItemInfoWindowClick = onClusterItemInfoWindowClick,
        onClusterItemInfoWindowLongClick = onClusterItemInfoWindowLongClick,
        clusterContent = clusterContent,
        clusterItemContent = clusterItemContent,
        clusterContentAnchor = clusterContentAnchor,
        clusterItemContentAnchor = clusterItemContentAnchor,
        clusterContentZIndex = clusterContentZIndex,
        clusterItemContentZIndex = clusterItemContentZIndex,
        clusterContentRotation = clusterContentRotation,
        clusterItemContentRotation = clusterItemContentRotation,
        clusterItemDecoration = clusterItemDecoration,
        onClusterManager = null,
        key = key,
    )
}

/**
 * Groups many items on a map based on zoom level.
 *
 * If a cluster or item has an info window, the map shows it when the user clicks its marker and
 * [onClusterClick] or [onClusterItemClick] returns false. The map also shows it when code calls
 * [Marker.showInfoWindow] on the marker. What each info window parameter draws must have a
 * non-zero width and height for every cluster and item that it gets. Otherwise, rendering the info
 * window throws an [IllegalStateException].
 *
 * @param items all items to show
 * @param onClusterClick a lambda invoked when the user clicks a cluster of items
 * @param onClusterItemClick a lambda invoked when the user clicks a non-clustered item
 * @param onClusterItemInfoWindowClick a lambda invoked when the user clicks the info window of a
 * non-clustered item
 * @param onClusterItemInfoWindowLongClick a lambda invoked when the user long-clicks the info
 * window of a non-clustered item
 * @param clusterContent an optional Composable that is rendered for each [Cluster].
 * @param clusterItemContent an optional Composable that is rendered for each non-clustered item.
 * @param clusterContentAnchor the anchor for the cluster image
 * @param clusterItemContentAnchor the anchor for the non-clustered item image
 * @param clusterContentZIndex the z-index of the cluster
 * @param clusterItemContentZIndex the z-index of the non-clustered item
 * @param clusterContentRotation the rotation of the cluster in degrees clockwise about the marker's anchor point
 * @param clusterItemContentRotation the rotation of the non-clustered item in degrees clockwise about the marker's anchor point
 * @param key an optional stable, unique key for each item. When supplied, items with the same key
 * are treated as updates when their values differ. Without a key, items are identified by their
 * `equals` and `hashCode` implementations. Duplicate keys in a collection produce a warning
 * and only the last item for each key is kept.
 * @param clusterItemDecoration optional map content drawn for each item while it isn't part of a cluster
 * @param onClusterInfoWindowClick an optional lambda invoked when the user clicks the info window
 * of a cluster. If this value is null, Clustering keeps any listener that other code set on the
 * cluster manager.
 * @param onClusterInfoWindowLongClick an optional lambda invoked when the user long-clicks the info
 * window of a cluster. If this value is null, Clustering keeps any listener that other code set on
 * the cluster manager.
 * @param clusterInfoWindow optional composable lambda expression for customizing the entire info
 * window of a cluster. If this value is not null, the map does not use [clusterInfoWindowContent].
 * @param clusterInfoWindowContent optional composable lambda expression for customizing the
 * content of a cluster's info window. The map shows this content inside the default info window
 * frame.
 * @param clusterItemInfoWindow optional composable lambda expression for customizing the entire
 * info window of a non-clustered item. If this value is not null, the map does not use
 * [clusterItemInfoWindowContent].
 * @param clusterItemInfoWindowContent optional composable lambda expression for customizing the
 * content of a non-clustered item's info window. The map shows this content inside the default
 * info window frame.
 * @param onClusterManager an optional lambda invoked with the clusterManager as a param when both
 * the clusterManager and renderer are set up, allowing callers a customization hook.
 */
@Composable
@GoogleMapComposable
@MapsComposeExperimentalApi
public fun <T : ClusterItem> Clustering(
    items: Collection<T>,
    onClusterClick: (Cluster<T>) -> Boolean = { false },
    onClusterItemClick: (T) -> Boolean = { false },
    onClusterItemInfoWindowClick: (T) -> Unit = { },
    onClusterItemInfoWindowLongClick: (T) -> Unit = { },
    clusterContent: @[UiComposable Composable] ((Cluster<T>) -> Unit)? = null,
    clusterItemContent: @[UiComposable Composable] ((T) -> Unit)? = null,
    clusterContentAnchor: Offset = Offset(0.5f, 1.0f),
    clusterItemContentAnchor: Offset = Offset(0.5f, 1.0f),
    clusterContentZIndex: Float = 0.0f,
    clusterItemContentZIndex: Float = 0.0f,
    clusterContentRotation: Float = 0.0f,
    clusterItemContentRotation: Float = 0.0f,
    key: ((T) -> Any)? = null,
    clusterItemDecoration:
    @Composable @GoogleMapComposable
    (T) -> Unit = {},
    onClusterInfoWindowClick: ((Cluster<T>) -> Unit)? = null,
    onClusterInfoWindowLongClick: ((Cluster<T>) -> Unit)? = null,
    clusterInfoWindow: @[UiComposable Composable] ((Cluster<T>) -> Unit)? = null,
    clusterInfoWindowContent: @[UiComposable Composable] ((Cluster<T>) -> Unit)? = null,
    clusterItemInfoWindow: @[UiComposable Composable] ((T) -> Unit)? = null,
    clusterItemInfoWindowContent: @[UiComposable Composable] ((T) -> Unit)? = null,
    onClusterManager: ((ClusterManager<T>) -> Unit)? = null,
) {
    val clusterManager = rememberClusterManager<T>()
    val renderer = rememberClusterRenderer(
        clusterContent,
        clusterItemContent,
        clusterContentAnchor,
        clusterItemContentAnchor,
        clusterContentZIndex,
        clusterItemContentZIndex,
        clusterContentRotation,
        clusterItemContentRotation,
        clusterManager,
    )

    SideEffect {
        clusterManager ?: return@SideEffect
        renderer ?: return@SideEffect

        if (clusterManager.renderer != renderer) {
            clusterManager.renderer = renderer
        }

        clusterManager.setOnClusterClickListener(onClusterClick)
        clusterManager.setOnClusterItemClickListener(onClusterItemClick)
        clusterManager.setOnClusterItemInfoWindowClickListener(onClusterItemInfoWindowClick)
        clusterManager.setOnClusterItemInfoWindowLongClickListener(onClusterItemInfoWindowLongClick)

        onClusterManager?.invoke(clusterManager)
    }

    if (clusterManager != null) {
        ClusterInfoWindowClickListeners(
            clusterManager = clusterManager,
            onClick = onClusterInfoWindowClick,
            onLongClick = onClusterInfoWindowLongClick,
        )
    }

    if (clusterManager != null && renderer != null) {
        Clustering(
            items = items,
            clusterManager = clusterManager,
            clusterItemDecoration = clusterItemDecoration,
            renderer = renderer,
            key = key,
            clusterInfoWindow = clusterInfoWindow,
            clusterInfoWindowContent = clusterInfoWindowContent,
            clusterItemInfoWindow = clusterItemInfoWindow,
            clusterItemInfoWindowContent = clusterItemInfoWindowContent,
        )
    }
}

/**
 * Sets [onClick] and [onLongClick] as the cluster info window listeners of [clusterManager] while
 * they are in the composition. When a callback leaves the composition, this function clears its
 * listener. A null callback does not change its listener, so a listener that other code set stays
 * in place.
 */
@Composable
internal fun <T : ClusterItem> ClusterInfoWindowClickListeners(
    clusterManager: ClusterManager<T>,
    onClick: ((Cluster<T>) -> Unit)?,
    onLongClick: ((Cluster<T>) -> Unit)?,
) {
    if (onClick != null) {
        val currentOnClick by rememberUpdatedState(onClick)
        DisposableEffect(clusterManager) {
            clusterManager.setOnClusterInfoWindowClickListener { currentOnClick(it) }
            onDispose { clusterManager.setOnClusterInfoWindowClickListener(null) }
        }
    }
    if (onLongClick != null) {
        val currentOnLongClick by rememberUpdatedState(onLongClick)
        DisposableEffect(clusterManager) {
            clusterManager.setOnClusterInfoWindowLongClickListener { currentOnLongClick(it) }
            onDispose { clusterManager.setOnClusterInfoWindowLongClickListener(null) }
        }
    }
}

/**
 * Groups many items on a map based on clusterManager.
 *
 * @param items all items to show
 * @param clusterManager a [ClusterManager] that can be used to specify the algorithm used by the rendering.
 * @param key an optional stable, unique key for each item. When supplied, items with the same key
 * are treated as updates when their values differ. Without a key, items are identified by their
 * `equals` and `hashCode` implementations. Duplicate keys in a collection produce a warning
 * and only the last item for each key is kept.
 * @param clusterItemDecoration optional map content drawn for each item while it isn't part of a cluster
 */
@Composable
@GoogleMapComposable
@MapsComposeExperimentalApi
public fun <T : ClusterItem> Clustering(
    items: Collection<T>,
    clusterManager: ClusterManager<T>,
    key: ((T) -> Any)? = null,
    clusterItemDecoration:
    @Composable @GoogleMapComposable
    (T) -> Unit = {},
) {
    Clustering(
        items = items,
        clusterManager = clusterManager,
        clusterItemDecoration = clusterItemDecoration,
        renderer = null,
        key = key,
    )
}

/** Retains the pre-key JVM signature for already compiled callers. */
@Deprecated("Retained for binary compatibility", level = DeprecationLevel.HIDDEN)
@Suppress("DEPRECATION")
@Composable
@GoogleMapComposable
@MapsComposeExperimentalApi
public fun <T : ClusterItem> Clustering(
    items: Collection<T>,
    onClusterClick: (Cluster<T>) -> Boolean = { false },
    onClusterItemClick: (T) -> Boolean = { false },
    onClusterItemInfoWindowClick: (T) -> Unit = { },
    onClusterItemInfoWindowLongClick: (T) -> Unit = { },
    clusterContent: @[UiComposable Composable] ((Cluster<T>) -> Unit)? = null,
    clusterItemContent: @[UiComposable Composable] ((T) -> Unit)? = null,
    clusterContentAnchor: Offset = Offset(0.5f, 1.0f),
    clusterItemContentAnchor: Offset = Offset(0.5f, 1.0f),
    clusterContentZIndex: Float = 0.0f,
    clusterItemContentZIndex: Float = 0.0f,
    clusterContentRotation: Float = 0.0f,
    clusterItemContentRotation: Float = 0.0f,
    clusterRenderer: ClusterRenderer<T>? = null,
    clusterItemDecoration:
    @Composable @GoogleMapComposable
    (T) -> Unit = {},
): Unit = Clustering(
    items = items,
    onClusterClick = onClusterClick,
    onClusterItemClick = onClusterItemClick,
    onClusterItemInfoWindowClick = onClusterItemInfoWindowClick,
    onClusterItemInfoWindowLongClick = onClusterItemInfoWindowLongClick,
    clusterContent = clusterContent,
    clusterItemContent = clusterItemContent,
    clusterContentAnchor = clusterContentAnchor,
    clusterItemContentAnchor = clusterItemContentAnchor,
    clusterContentZIndex = clusterContentZIndex,
    clusterItemContentZIndex = clusterItemContentZIndex,
    clusterContentRotation = clusterContentRotation,
    clusterItemContentRotation = clusterItemContentRotation,
    clusterRenderer = clusterRenderer,
    key = null,
    clusterItemDecoration = clusterItemDecoration,
)

/** Retains the pre-key JVM signature for already compiled callers. */
@Deprecated("Retained for binary compatibility", level = DeprecationLevel.HIDDEN)
@Composable
@GoogleMapComposable
@MapsComposeExperimentalApi
public fun <T : ClusterItem> Clustering(
    items: Collection<T>,
    onClusterClick: (Cluster<T>) -> Boolean = { false },
    onClusterItemClick: (T) -> Boolean = { false },
    onClusterItemInfoWindowClick: (T) -> Unit = { },
    onClusterItemInfoWindowLongClick: (T) -> Unit = { },
    clusterContent: @[UiComposable Composable] ((Cluster<T>) -> Unit)? = null,
    clusterItemContent: @[UiComposable Composable] ((T) -> Unit)? = null,
    clusterContentAnchor: Offset = Offset(0.5f, 1.0f),
    clusterItemContentAnchor: Offset = Offset(0.5f, 1.0f),
    clusterContentZIndex: Float = 0.0f,
    clusterItemContentZIndex: Float = 0.0f,
    clusterContentRotation: Float = 0.0f,
    clusterItemContentRotation: Float = 0.0f,
    clusterItemDecoration:
    @Composable @GoogleMapComposable
    (T) -> Unit = {},
): Unit = Clustering(
    items = items,
    onClusterClick = onClusterClick,
    onClusterItemClick = onClusterItemClick,
    onClusterItemInfoWindowClick = onClusterItemInfoWindowClick,
    onClusterItemInfoWindowLongClick = onClusterItemInfoWindowLongClick,
    clusterContent = clusterContent,
    clusterItemContent = clusterItemContent,
    clusterContentAnchor = clusterContentAnchor,
    clusterItemContentAnchor = clusterItemContentAnchor,
    clusterContentZIndex = clusterContentZIndex,
    clusterItemContentZIndex = clusterItemContentZIndex,
    clusterContentRotation = clusterContentRotation,
    clusterItemContentRotation = clusterItemContentRotation,
    key = null,
    clusterItemDecoration = clusterItemDecoration,
)

/** Retains the pre-key JVM signature for already compiled callers. */
@Deprecated("Retained for binary compatibility", level = DeprecationLevel.HIDDEN)
@Composable
@GoogleMapComposable
@MapsComposeExperimentalApi
public fun <T : ClusterItem> Clustering(
    items: Collection<T>,
    onClusterClick: (Cluster<T>) -> Boolean = { false },
    onClusterItemClick: (T) -> Boolean = { false },
    onClusterItemInfoWindowClick: (T) -> Unit = { },
    onClusterItemInfoWindowLongClick: (T) -> Unit = { },
    clusterContent: @[UiComposable Composable] ((Cluster<T>) -> Unit)? = null,
    clusterItemContent: @[UiComposable Composable] ((T) -> Unit)? = null,
    clusterContentAnchor: Offset = Offset(0.5f, 1.0f),
    clusterItemContentAnchor: Offset = Offset(0.5f, 1.0f),
    clusterContentZIndex: Float = 0.0f,
    clusterItemContentZIndex: Float = 0.0f,
    clusterContentRotation: Float = 0.0f,
    clusterItemContentRotation: Float = 0.0f,
    clusterItemDecoration:
    @Composable @GoogleMapComposable
    (T) -> Unit = {},
    onClusterManager: ((ClusterManager<T>) -> Unit)? = null,
): Unit = Clustering(
    items = items,
    onClusterClick = onClusterClick,
    onClusterItemClick = onClusterItemClick,
    onClusterItemInfoWindowClick = onClusterItemInfoWindowClick,
    onClusterItemInfoWindowLongClick = onClusterItemInfoWindowLongClick,
    clusterContent = clusterContent,
    clusterItemContent = clusterItemContent,
    clusterContentAnchor = clusterContentAnchor,
    clusterItemContentAnchor = clusterItemContentAnchor,
    clusterContentZIndex = clusterContentZIndex,
    clusterItemContentZIndex = clusterItemContentZIndex,
    clusterContentRotation = clusterContentRotation,
    clusterItemContentRotation = clusterItemContentRotation,
    key = null,
    clusterItemDecoration = clusterItemDecoration,
    onClusterManager = onClusterManager,
)

/** Retains the pre-key JVM signature for already compiled callers. */
@Deprecated("Retained for binary compatibility", level = DeprecationLevel.HIDDEN)
@Composable
@GoogleMapComposable
@MapsComposeExperimentalApi
public fun <T : ClusterItem> Clustering(
    items: Collection<T>,
    clusterManager: ClusterManager<T>,
    clusterItemDecoration:
    @Composable @GoogleMapComposable
    (T) -> Unit = {},
): Unit = Clustering(
    items = items,
    clusterManager = clusterManager,
    key = null,
    clusterItemDecoration = clusterItemDecoration,
)

/** Retains the pre-cluster-info-window JVM signature for already compiled callers. */
@Deprecated("Retained for binary compatibility", level = DeprecationLevel.HIDDEN)
@Composable
@GoogleMapComposable
@MapsComposeExperimentalApi
public fun <T : ClusterItem> Clustering(
    items: Collection<T>,
    onClusterClick: (Cluster<T>) -> Boolean = { false },
    onClusterItemClick: (T) -> Boolean = { false },
    onClusterItemInfoWindowClick: (T) -> Unit = { },
    onClusterItemInfoWindowLongClick: (T) -> Unit = { },
    clusterContent: @[UiComposable Composable] ((Cluster<T>) -> Unit)? = null,
    clusterItemContent: @[UiComposable Composable] ((T) -> Unit)? = null,
    clusterContentAnchor: Offset = Offset(0.5f, 1.0f),
    clusterItemContentAnchor: Offset = Offset(0.5f, 1.0f),
    clusterContentZIndex: Float = 0.0f,
    clusterItemContentZIndex: Float = 0.0f,
    clusterContentRotation: Float = 0.0f,
    clusterItemContentRotation: Float = 0.0f,
    key: ((T) -> Any)? = null,
    clusterItemDecoration:
    @Composable @GoogleMapComposable
    (T) -> Unit = {},
    onClusterManager: ((ClusterManager<T>) -> Unit)? = null,
): Unit = Clustering(
    items = items,
    onClusterClick = onClusterClick,
    onClusterItemClick = onClusterItemClick,
    onClusterItemInfoWindowClick = onClusterItemInfoWindowClick,
    onClusterItemInfoWindowLongClick = onClusterItemInfoWindowLongClick,
    clusterContent = clusterContent,
    clusterItemContent = clusterItemContent,
    clusterContentAnchor = clusterContentAnchor,
    clusterItemContentAnchor = clusterItemContentAnchor,
    clusterContentZIndex = clusterContentZIndex,
    clusterItemContentZIndex = clusterItemContentZIndex,
    clusterContentRotation = clusterContentRotation,
    clusterItemContentRotation = clusterItemContentRotation,
    key = key,
    clusterItemDecoration = clusterItemDecoration,
    onClusterManager = onClusterManager,
)

@Composable
@GoogleMapComposable
@MapsComposeExperimentalApi
internal fun <T : ClusterItem> Clustering(
    items: Collection<T>,
    clusterManager: ClusterManager<T>,
    clusterItemDecoration:
    @Composable @GoogleMapComposable
    (T) -> Unit = {},
    renderer: ClusterRenderer<T>? = null,
    key: ((T) -> Any)? = null,
    clusterInfoWindow: (@Composable (Cluster<T>) -> Unit)? = null,
    clusterInfoWindowContent: (@Composable (Cluster<T>) -> Unit)? = null,
    clusterItemInfoWindow: (@Composable (T) -> Unit)? = null,
    clusterItemInfoWindowContent: (@Composable (T) -> Unit)? = null,
) {
    ResetMapListeners(clusterManager)
    InputHandler(
        onMarkerClick = clusterManager.markerManager::onMarkerClick,
        onInfoWindowClick = clusterManager.markerManager::onInfoWindowClick,
        onInfoWindowLongClick = clusterManager.markerManager::onInfoWindowLongClick,
        onMarkerDrag = clusterManager.markerManager::onMarkerDrag,
        onMarkerDragEnd = clusterManager.markerManager::onMarkerDragEnd,
        onMarkerDragStart = clusterManager.markerManager::onMarkerDragStart,
    )
    clusterManager.infoWindowLookups(
        clusterInfoWindow = clusterInfoWindow,
        clusterInfoWindowContent = clusterInfoWindowContent,
        clusterItemInfoWindow = clusterItemInfoWindow,
        clusterItemInfoWindowContent = clusterItemInfoWindowContent,
    )?.let { lookups ->
        InfoWindowHandler(infoWindow = lookups.infoWindow, infoContent = lookups.infoContent)
    }
    val cameraPositionState = currentCameraPositionState
    LaunchedEffect(cameraPositionState, clusterManager) {
        snapshotFlow { cameraPositionState.isMoving }
            .collect { isMoving ->
                if (!isMoving) {
                    clusterManager.onCameraIdle()
                }
            }
    }
    val itemsState = rememberUpdatedState(items)
    val keyState = rememberUpdatedState(key)
    LaunchedEffect(clusterManager, itemsState, keyState) {
        var previousKeyedItems: Map<Any, T>? = null
        var previousUsedExplicitKey: Boolean? = null
        snapshotFlow { itemsState.value.toList() to keyState.value }
            .collect { (items, itemKey) ->
                val currentKeyedItems = clusterItemsByKey(items, itemKey) { duplicateKey ->
                    Log.w("Clustering", "Duplicate item key: $duplicateKey. Keeping the last item.")
                }
                val previousItems = previousKeyedItems
                val usesExplicitKey = itemKey != null

                if (previousItems == null || previousUsedExplicitKey != usesExplicitKey) {
                    // Reset on the first update or when switching identity strategies.
                    clusterManager.clearItems()
                    clusterManager.addItems(currentKeyedItems.values)
                    clusterManager.cluster()
                } else {
                    val diff = diffClusterItems(previousItems, currentKeyedItems)
                    val removedItems = diff.removed + diff.updated.map { (oldItem, _) -> oldItem }
                    val addedItems = diff.added + diff.updated.map { (_, newItem) -> newItem }
                    if (removedItems.isNotEmpty()) {
                        clusterManager.removeItems(removedItems)
                    }
                    if (addedItems.isNotEmpty()) {
                        clusterManager.addItems(addedItems)
                    }
                    if (diff.hasChanges) {
                        clusterManager.cluster()
                    }
                }
                previousKeyedItems = currentKeyedItems
                previousUsedExplicitKey = usesExplicitKey
            }
    }
    DisposableEffect(clusterManager) {
        onDispose {
            clusterManager.clearItems()
            clusterManager.cluster()
        }
    }

    val actualRenderer = renderer ?: clusterManager.renderer

    @Suppress("UNCHECKED_CAST")
    val unclusteredItems by (actualRenderer as? ClusterRendererItemState<T>)?.unclusteredItems
        ?: remember { mutableStateOf(emptySet()) }
    for (item in unclusteredItems) {
        clusterItemDecoration(item)
    }
}

/** The lookups an [InfoWindowHandler] uses to find a marker's info window and info content. */
internal class InfoWindowLookups(
    val infoWindow: (Marker) -> (@Composable () -> Unit)?,
    val infoContent: (Marker) -> (@Composable () -> Unit)?,
)

/**
 * Returns lookups that give the markers of this cluster manager the given info windows. Returns
 * null if all four content parameters are null. [InfoWindowLookups.infoWindow] uses the
 * whole-window content, and [InfoWindowLookups.infoContent] uses the in-frame content. Each lookup
 * uses the renderer that the cluster manager has at the time of the lookup.
 */
internal fun <T : ClusterItem> ClusterManager<T>.infoWindowLookups(
    clusterInfoWindow: (@Composable (Cluster<T>) -> Unit)?,
    clusterInfoWindowContent: (@Composable (Cluster<T>) -> Unit)?,
    clusterItemInfoWindow: (@Composable (T) -> Unit)?,
    clusterItemInfoWindowContent: (@Composable (T) -> Unit)?,
): InfoWindowLookups? {
    if (
        clusterInfoWindow == null &&
        clusterInfoWindowContent == null &&
        clusterItemInfoWindow == null &&
        clusterItemInfoWindowContent == null
    ) {
        return null
    }
    return InfoWindowLookups(
        infoWindow = { marker ->
            renderer.infoWindowContent(
                marker = marker,
                clusterContent = clusterInfoWindow,
                clusterItemContent = clusterItemInfoWindow,
            )
        },
        infoContent = { marker ->
            renderer.infoWindowContent(
                marker = marker,
                clusterContent = clusterInfoWindowContent,
                clusterItemContent = clusterItemInfoWindowContent,
            )
        },
    )
}

/**
 * Returns the info window content for [marker], bound to the cluster or item that this renderer
 * drew the marker for. A cluster's marker gets [clusterContent], and an item's marker gets
 * [clusterItemContent]. Returns null if this renderer did not draw [marker], if there is no content
 * for that kind of marker, or if this renderer is not a [DefaultClusterRenderer].
 */
internal fun <T : ClusterItem> ClusterRenderer<T>.infoWindowContent(
    marker: Marker,
    clusterContent: (@Composable (Cluster<T>) -> Unit)?,
    clusterItemContent: (@Composable (T) -> Unit)?,
): (@Composable () -> Unit)? {
    val renderer = this as? DefaultClusterRenderer<T> ?: return null
    val cluster = renderer.getCluster(marker)
    if (cluster != null) {
        return clusterContent?.let { content -> { content(cluster) } }
    }
    val item = renderer.getClusterItem(marker) ?: return null
    return clusterItemContent?.let { content -> { content(item) } }
}

/**
 * Default Renderer for drawing Cluster and ClusterItem markers.
 *
 * @param clusterManager the [ClusterManager] that manages the clusters
 * @param executor an optional [Executor] used by [DefaultClusterRenderer] to process view
 * modifications off the main thread. Must be remembered across recompositions (e.g. with [remember])
 * to avoid recreating the renderer on every recomposition. Tests can pass a direct executor to make
 * rendering deterministic.
 */
@Composable
@GoogleMapComposable
@MapsComposeExperimentalApi
public fun <T : ClusterItem> rememberClusterRenderer(
    clusterManager: ClusterManager<T>?,
    executor: Executor? = null,
): ClusterRenderer<T>? {
    val context = LocalContext.current
    val clusterRendererState: MutableState<ClusterRenderer<T>?> = remember { mutableStateOf(null) }

    clusterManager ?: return null
    MapEffect(context, clusterManager, executor) { map ->
        val renderer = if (executor != null) {
            ReportingDefaultClusterRenderer(
                context = context,
                map = map,
                clusterManager = clusterManager,
                executor = executor,
            )
        } else {
            ReportingDefaultClusterRenderer(
                context = context,
                map = map,
                clusterManager = clusterManager,
            )
        }
        clusterRendererState.value = renderer
    }

    return clusterRendererState.value
}

/**
 * Default Renderer for drawing Cluster and ClusterItem markers.
 *
 * Retained for binary compatibility with binaries compiled against earlier versions.
 */
@Deprecated(
    message = "Retained for binary compatibility.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
@GoogleMapComposable
@MapsComposeExperimentalApi
public fun <T : ClusterItem> rememberClusterRenderer(clusterManager: ClusterManager<T>?): ClusterRenderer<T>? =
    rememberClusterRenderer(
        clusterManager = clusterManager,
        executor = null,
    )

/**
 * Default Renderer for drawing Composable.
 *
 * @param clusterContent an optional Composable that is rendered for each [Cluster].
 * @param clusterItemContent an optional Composable that is rendered for each non-clustered item.
 * @param clusterContentAnchor the anchor for the cluster image
 * @param clusterItemContentAnchor the anchor for the non-clustered item image
 * @param clusterContentZIndex the z-index of the cluster
 * @param clusterItemContentZIndex the z-index of the non-clustered item
 * @param clusterContentRotation the rotation of the cluster in degrees clockwise about the marker's anchor point
 * @param clusterItemContentRotation the rotation of the non-clustered item in degrees clockwise about the marker's anchor point
 * @param clusterManager the [ClusterManager] that manages the clusters
 * @param executor an optional [Executor] used by [DefaultClusterRenderer] to process view
 * modifications off the main thread. Must be remembered across recompositions (e.g. with [remember])
 * to avoid recreating the renderer on every recomposition. Tests can pass a direct executor to make
 * rendering deterministic.
 */
@Composable
@GoogleMapComposable
@MapsComposeExperimentalApi
public fun <T : ClusterItem> rememberClusterRenderer(
    clusterContent: @Composable ((Cluster<T>) -> Unit)?,
    clusterItemContent: @Composable ((T) -> Unit)?,
    clusterContentAnchor: Offset = Offset(0.5f, 1.0f),
    clusterItemContentAnchor: Offset = Offset(0.5f, 1.0f),
    clusterContentZIndex: Float = 0.0f,
    clusterItemContentZIndex: Float = 0.0f,
    clusterContentRotation: Float = 0.0f,
    clusterItemContentRotation: Float = 0.0f,
    clusterManager: ClusterManager<T>?,
    executor: Executor? = null,
): ClusterRenderer<T>? {
    val clusterContentState = rememberUpdatedState(clusterContent)
    val clusterItemContentState = rememberUpdatedState(clusterItemContent)
    val clusterContentAnchorState = rememberUpdatedState(clusterContentAnchor)
    val clusterItemContentAnchorState = rememberUpdatedState(clusterItemContentAnchor)
    val clusterContentZIndexState = rememberUpdatedState(clusterContentZIndex)
    val clusterItemContentZIndexState = rememberUpdatedState(clusterItemContentZIndex)
    val clusterContentRotationState = rememberUpdatedState(clusterContentRotation)
    val clusterItemContentRotationState = rememberUpdatedState(clusterItemContentRotation)
    val context = LocalContext.current
    val viewRendererState = rememberUpdatedState(rememberComposeUiViewRenderer())
    val clusterRendererState: MutableState<ClusterRenderer<T>?> = remember { mutableStateOf(null) }

    clusterManager ?: return null
    MapEffect(context, clusterManager, executor) { map ->
        val renderer = if (executor != null) {
            ComposeUiClusterRenderer(
                context = context,
                scope = this,
                map = map,
                clusterManager = clusterManager,
                viewRendererState = viewRendererState,
                clusterContentState = clusterContentState,
                clusterItemContentState = clusterItemContentState,
                clusterContentAnchorState = clusterContentAnchorState,
                clusterItemContentAnchorState = clusterItemContentAnchorState,
                clusterContentZIndexState = clusterContentZIndexState,
                clusterItemContentZIndexState = clusterItemContentZIndexState,
                clusterContentRotationState = clusterContentRotationState,
                clusterItemContentRotationState = clusterItemContentRotationState,
                executor = executor,
            )
        } else {
            ComposeUiClusterRenderer(
                context = context,
                scope = this,
                map = map,
                clusterManager = clusterManager,
                viewRendererState = viewRendererState,
                clusterContentState = clusterContentState,
                clusterItemContentState = clusterItemContentState,
                clusterContentAnchorState = clusterContentAnchorState,
                clusterItemContentAnchorState = clusterItemContentAnchorState,
                clusterContentZIndexState = clusterContentZIndexState,
                clusterItemContentZIndexState = clusterItemContentZIndexState,
                clusterContentRotationState = clusterContentRotationState,
                clusterItemContentRotationState = clusterItemContentRotationState,
            )
        }
        clusterRendererState.value = renderer
        awaitCancellation()
    }
    return clusterRendererState.value
}

/**
 * Default Renderer for drawing Composable.
 *
 * Retained for binary compatibility with binaries compiled against earlier versions.
 */
@Deprecated(
    message = "Retained for binary compatibility.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
@GoogleMapComposable
@MapsComposeExperimentalApi
public fun <T : ClusterItem> rememberClusterRenderer(
    clusterContent: @Composable ((Cluster<T>) -> Unit)?,
    clusterItemContent: @Composable ((T) -> Unit)?,
    clusterContentAnchor: Offset = Offset(0.5f, 1.0f),
    clusterItemContentAnchor: Offset = Offset(0.5f, 1.0f),
    clusterContentZIndex: Float = 0.0f,
    clusterItemContentZIndex: Float = 0.0f,
    clusterContentRotation: Float = 0.0f,
    clusterItemContentRotation: Float = 0.0f,
    clusterManager: ClusterManager<T>?,
): ClusterRenderer<T>? = rememberClusterRenderer(
    clusterContent = clusterContent,
    clusterItemContent = clusterItemContent,
    clusterContentAnchor = clusterContentAnchor,
    clusterItemContentAnchor = clusterItemContentAnchor,
    clusterContentZIndex = clusterContentZIndex,
    clusterItemContentZIndex = clusterItemContentZIndex,
    clusterContentRotation = clusterContentRotation,
    clusterItemContentRotation = clusterItemContentRotation,
    clusterManager = clusterManager,
    executor = null,
)

/**
 * Remembers a [ClusterManager] configured with the given coroutine contexts.
 *
 * @param mainContext the coroutine context used to deliver clusters to the renderer. It must
 * dispatch on the main thread. Custom contexts (e.g. `Dispatchers.Main + job`) must be remembered
 * across recompositions (e.g. with [remember]) to avoid recreating the manager on every recomposition.
 * @param backgroundContext the coroutine context used to compute clusters off the main thread.
 * Custom contexts (e.g. `Dispatchers.Default + job`) must be remembered across recompositions (e.g.
 * with [remember]) to avoid recreating the manager on every recomposition. Tests can pass a test
 * dispatcher for both contexts to make clustering deterministic.
 */
@Composable
@GoogleMapComposable
@MapsComposeExperimentalApi
public fun <T : ClusterItem> rememberClusterManager(
    mainContext: CoroutineContext = Dispatchers.Main,
    backgroundContext: CoroutineContext = Dispatchers.Default,
): ClusterManager<T>? {
    val context = LocalContext.current
    val clusterManagerState: MutableState<ClusterManager<T>?> = remember { mutableStateOf(null) }
    MapEffect(context, mainContext, backgroundContext) { map ->
        clusterManagerState.value = ClusterManager<T>(context, map, mainContext, backgroundContext)
    }
    return clusterManagerState.value
}

/**
 * Remembers a [ClusterManager].
 *
 * Retained for binary compatibility with binaries compiled against earlier versions.
 */
@Deprecated(
    message = "Retained for binary compatibility.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
@GoogleMapComposable
@MapsComposeExperimentalApi
public fun <T : ClusterItem> rememberClusterManager(): ClusterManager<T>? = rememberClusterManager(
    mainContext = Dispatchers.Main,
    backgroundContext = Dispatchers.Default,
)

@OptIn(MapsComposeExperimentalApi::class)
@Composable
private fun <T : ClusterItem> rememberClusterManager(
    clusterContent: @Composable ((Cluster<T>) -> Unit)?,
    clusterItemContent: @Composable ((T) -> Unit)?,
    clusterContentAnchor: Offset = Offset(0.5f, 1.0f),
    clusterItemContentAnchor: Offset = Offset(0.5f, 1.0f),
    clusterContentZIndex: Float = 0.0f,
    clusterItemContentZIndex: Float = 0.0f,
    clusterContentRotation: Float = 0.0f,
    clusterItemContentRotation: Float = 0.0f,
    clusterRenderer: ClusterRenderer<T>? = null,
): ClusterManager<T>? {
    val clusterContentState = rememberUpdatedState(clusterContent)
    val clusterItemContentState = rememberUpdatedState(clusterItemContent)
    val clusterContentAnchorState = rememberUpdatedState(clusterContentAnchor)
    val clusterItemContentAnchorState = rememberUpdatedState(clusterItemContentAnchor)
    val clusterContentZIndexState = rememberUpdatedState(clusterContentZIndex)
    val clusterItemContentZIndexState = rememberUpdatedState(clusterItemContentZIndex)
    val clusterContentRotationState = rememberUpdatedState(clusterContentRotation)
    val clusterItemContentRotationState = rememberUpdatedState(clusterItemContentRotation)
    val context = LocalContext.current
    val viewRendererState = rememberUpdatedState(rememberComposeUiViewRenderer())
    val clusterManagerState: MutableState<ClusterManager<T>?> = remember { mutableStateOf(null) }
    MapEffect(context) { map ->
        val clusterManager = ClusterManager<T>(context, map)

        launch {
            snapshotFlow {
                clusterContentState.value != null || clusterItemContentState.value != null
            }
                .collect { hasCustomContent ->
                    val renderer = clusterRenderer
                        ?: if (hasCustomContent) {
                            ComposeUiClusterRenderer<T>(
                                context,
                                scope = this,
                                map,
                                clusterManager,
                                viewRendererState,
                                clusterContentState,
                                clusterItemContentState,
                                clusterContentAnchorState,
                                clusterItemContentAnchorState,
                                clusterContentZIndexState,
                                clusterItemContentZIndexState,
                                clusterContentRotationState,
                                clusterItemContentRotationState,
                            )
                        } else {
                            ReportingDefaultClusterRenderer(context, map, clusterManager)
                        }
                    clusterManager.renderer = renderer
                }
        }

        clusterManagerState.value = clusterManager
    }
    return clusterManagerState.value
}

/**
 * This is a hack.
 * [ClusterManager] instantiates a [MarkerManager], which posts a runnable to the UI thread that
 * overwrites a bunch of [GoogleMap]'s listeners. Many Maps composables rely on those listeners
 * being set by [com.google.maps.android.compose.MapApplier].
 * This posts _another_ runnable which effectively undoes that, signaling MapApplier to set the
 * listeners again.
 * This is heavily coupled to implementation details of [MarkerManager].
 */
@Composable
private fun ResetMapListeners(clusterManager: ClusterManager<*>) {
    val reattach = rememberReattachClickListenersHandle()
    LaunchedEffect(clusterManager, reattach) {
        Handler(Looper.getMainLooper()).post {
            reattach()
        }
    }
}

internal class ReportingDefaultClusterRenderer<T : ClusterItem> :
    DefaultClusterRenderer<T>,
    ClusterRendererItemState<T> {

    override val unclusteredItems = mutableStateOf(emptySet<T>())

    constructor(
        context: Context,
        map: GoogleMap,
        clusterManager: ClusterManager<T>,
    ) : super(context, map, clusterManager)

    constructor(
        context: Context,
        map: GoogleMap,
        clusterManager: ClusterManager<T>,
        executor: Executor,
    ) : super(context, map, clusterManager, executor)

    override fun onClustersChanged(clusters: Set<Cluster<T>>) {
        super.onClustersChanged(clusters)
        unclusteredItems.value = clusters.filter { !shouldRenderAsCluster(it) }
            .flatMap { it.items }
            .toSet()
    }
}
