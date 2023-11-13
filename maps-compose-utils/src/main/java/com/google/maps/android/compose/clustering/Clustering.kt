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
import androidx.compose.ui.UiComposable
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
import kotlinx.coroutines.launch

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
            if (clusterManager != null) {
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
    clusterRenderer: ClusterRenderer<T>? = null,
) {
    val clusterManager = rememberClusterManager(clusterContent, clusterItemContent, clusterRenderer)
        ?: return

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
) {
    val clusterManager = rememberClusterManager<T>()
    val renderer = rememberClusterRenderer(clusterContent, clusterItemContent, clusterManager)
    SideEffect {
        if (clusterManager?.renderer != renderer) {
            clusterManager?.renderer = renderer ?: return@SideEffect
        }
    }

    SideEffect {
        clusterManager ?: return@SideEffect
        clusterManager.setOnClusterClickListener(onClusterClick)
        clusterManager.setOnClusterItemClickListener(onClusterItemClick)
        clusterManager.setOnClusterItemInfoWindowClickListener(onClusterItemInfoWindowClick)
        clusterManager.setOnClusterItemInfoWindowLongClickListener(onClusterItemInfoWindowLongClick)
    }

    if (clusterManager != null) {
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
 */
@Composable
@GoogleMapComposable
@MapsComposeExperimentalApi
public fun <T : ClusterItem> rememberClusterRenderer(
    clusterContent: @Composable ((Cluster<T>) -> Unit)?,
    clusterItemContent: @Composable ((T) -> Unit)?,
    clusterManager: ClusterManager<T>?,
): ClusterRenderer<T>? {
    val clusterContentState = rememberUpdatedState(clusterContent)
    val clusterItemContentState = rememberUpdatedState(clusterItemContent)
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
        )
        clusterRendererState.value = renderer
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
    clusterRenderer: ClusterRenderer<T>? = null,
): ClusterManager<T>? {
    val clusterContentState = rememberUpdatedState(clusterContent)
    val clusterItemContentState = rememberUpdatedState(clusterItemContent)
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
                            ComposeUiClusterRenderer(
                                context,
                                scope = this,
                                map,
                                clusterManager,
                                viewRendererState,
                                clusterContentState,
                                clusterItemContentState,
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
