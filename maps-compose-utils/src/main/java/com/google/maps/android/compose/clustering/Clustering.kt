package com.google.maps.android.compose.clustering

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.UiComposable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.GoogleMap
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.ClusterRenderer
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.collections.MarkerManager
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.InputHandler
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.currentCameraPositionState
import com.google.maps.android.compose.rememberComposeUiViewRenderer
import com.google.maps.android.compose.rememberReattachClickListenersHandle
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch

/**
 * Properties for a marker in [Clustering].
 */
public class ClusteringMarkerProperties {
    public var anchor: Offset? by mutableStateOf(null)
        internal set
    public var zIndex: Float? by mutableStateOf(null)
        internal set
}

/**
 * [CompositionLocal] used to provide [ClusteringMarkerProperties] to the content of a cluster or
 * cluster item.
 */
public val LocalClusteringMarkerProperties: androidx.compose.runtime.ProvidableCompositionLocal<ClusteringMarkerProperties> =
    staticCompositionLocalOf { ClusteringMarkerProperties() }

/**
 * Helper function to specify properties for the marker representing a cluster or cluster item.
 *
 * @param anchor the anchor for the marker image. If null, the default anchor specified in
 * [Clustering] will be used.
 * @param zIndex the z-index of the marker. If null, the default z-index specified in [Clustering]
 * will be used.
 */
@Composable
public fun clusteringMarkerProperties(
    anchor: Offset? = null,
    zIndex: Float? = null,
) {
    val properties = LocalClusteringMarkerProperties.current
    SideEffect {
        properties.anchor = anchor
        properties.zIndex = zIndex
    }
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
 * @param clusterRenderer an optional ClusterRenderer that can be used to specify the algorithm used by the rendering.
 */
@Composable
@GoogleMapComposable
@MapsComposeExperimentalApi
@Deprecated(
    message = "If clusterRenderer is specified, clusterContent and clusterItemContent are not used; use a function that takes ClusterManager as an argument instead.",
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
    clusterRenderer: ClusterRenderer<T>? = null,
) {
    val clusterManager = rememberClusterManager(
        clusterContent,
        clusterItemContent,
        clusterContentAnchor,
        clusterItemContentAnchor,
        clusterContentZIndex,
        clusterItemContentZIndex,
        clusterRenderer
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
        onClusterManager = null,
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
        clusterManager
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

    if (clusterManager != null && renderer != null) {
        Clustering(
            items = items,
            clusterManager = clusterManager,
        )
    }
}

/**
 * Groups many items on a map based on clusterManager.
 *
 * @param items all items to show
 * @param clusterManager a [ClusterManager] that can be used to specify the algorithm used by the rendering.
 */
@Composable
@GoogleMapComposable
@MapsComposeExperimentalApi
public fun <T : ClusterItem> Clustering(
    items: Collection<T>,
    clusterManager: ClusterManager<T>,
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
    val cameraPositionState = currentCameraPositionState
    LaunchedEffect(cameraPositionState) {
        snapshotFlow { cameraPositionState.isMoving }
            .collect { isMoving ->
                if (!isMoving) {
                    clusterManager.onCameraIdle()
                }
            }
    }
    val itemsState = rememberUpdatedState(items)
    LaunchedEffect(itemsState) {
        snapshotFlow { itemsState.value.toList() }
            .collect { items ->
                clusterManager.clearItems()
                clusterManager.addItems(items)
                clusterManager.cluster()
            }
    }
    DisposableEffect(itemsState) {
        onDispose {
            clusterManager.clearItems()
            clusterManager.cluster()
        }
    }
}


@Composable
@GoogleMapComposable
@MapsComposeExperimentalApi
public fun <T : ClusterItem> rememberClusterRenderer(
    clusterManager: ClusterManager<T>?,
): ClusterRenderer<T>? {
    val context = LocalContext.current
    val clusterRendererState: MutableState<ClusterRenderer<T>?> = remember { mutableStateOf(null) }

    clusterManager ?: return null
    MapEffect(context) { map ->
        val renderer = DefaultClusterRenderer(context, map, clusterManager)
        clusterRendererState.value = renderer
    }

    return clusterRendererState.value
}

/**
 * Default Renderer for drawing Composable.
 *
 * @param clusterContent an optional Composable that is rendered for each [Cluster].
 * @param clusterItemContent an optional Composable that is rendered for each non-clustered item.
 * @param clusterContentAnchor the anchor for the cluster image
 * @param clusterItemContentAnchor the anchor for the non-clustered item image
 * @param clusterContentZIndex the z-index of the cluster
 * @param clusterItemContentZIndex the z-index of the non-clustered item
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
    clusterManager: ClusterManager<T>?,
): ClusterRenderer<T>? {
    val clusterContentState = rememberUpdatedState(clusterContent)
    val clusterItemContentState = rememberUpdatedState(clusterItemContent)
    val clusterContentAnchorState = rememberUpdatedState(clusterContentAnchor)
    val clusterItemContentAnchorState = rememberUpdatedState(clusterItemContentAnchor)
    val clusterContentZIndexState = rememberUpdatedState(clusterContentZIndex)
    val clusterItemContentZIndexState = rememberUpdatedState(clusterItemContentZIndex)
    val context = LocalContext.current
    val viewRendererState = rememberUpdatedState(rememberComposeUiViewRenderer())
    val clusterRendererState: MutableState<ClusterRenderer<T>?> = remember { mutableStateOf(null) }

    clusterManager ?: return null
    MapEffect(context) { map ->
        val renderer = ComposeUiClusterRenderer(
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
        )
        clusterRendererState.value = renderer
        awaitCancellation()
    }
    return clusterRendererState.value
}

@Composable
@GoogleMapComposable
@MapsComposeExperimentalApi
public fun <T : ClusterItem> rememberClusterManager(): ClusterManager<T>? {
    val context = LocalContext.current
    val clusterManagerState: MutableState<ClusterManager<T>?> = remember { mutableStateOf(null) }
    MapEffect(context) { map ->
        clusterManagerState.value = ClusterManager<T>(context, map)
    }
    return clusterManagerState.value
}

@OptIn(MapsComposeExperimentalApi::class)
@Composable
private fun <T : ClusterItem> rememberClusterManager(
    clusterContent: @Composable ((Cluster<T>) -> Unit)?,
    clusterItemContent: @Composable ((T) -> Unit)?,
    clusterContentAnchor: Offset = Offset(0.5f, 1.0f),
    clusterItemContentAnchor: Offset = Offset(0.5f, 1.0f),
    clusterContentZIndex: Float = 0.0f,
    clusterItemContentZIndex: Float = 0.0f,
    clusterRenderer: ClusterRenderer<T>? = null,
): ClusterManager<T>? {
    val clusterContentState = rememberUpdatedState(clusterContent)
    val clusterItemContentState = rememberUpdatedState(clusterItemContent)
    val clusterContentAnchorState = rememberUpdatedState(clusterContentAnchor)
    val clusterItemContentAnchorState = rememberUpdatedState(clusterItemContentAnchor)
    val clusterContentZIndexState = rememberUpdatedState(clusterContentZIndex)
    val clusterItemContentZIndexState = rememberUpdatedState(clusterItemContentZIndex)
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
                            )
                        } else {
                            DefaultClusterRenderer(context, map, clusterManager)
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
private fun ResetMapListeners(
    clusterManager: ClusterManager<*>,
) {
    val reattach = rememberReattachClickListenersHandle()
    LaunchedEffect(clusterManager, reattach) {
        Handler(Looper.getMainLooper()).post {
            reattach()
        }
    }
}
