package com.google.maps.android.compose.clustering

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.GoogleMap
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.collections.MarkerManager
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.InputHandler
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.rememberReattachClickListenersHandle

/**
 * Groups many items on a map based on zoom level.
 *
 * @param items all items to show
 * @param cameraPositionState the state of the camera, which affects clustering behavior
 * @param onClusterClick a lambda invoked when the user clicks a cluster of items
 * @param onClusterItemClick a lambda invoked when the user clicks a non-clustered item
 * @param onClusterItemInfoWindowClick a lambda invoked when the user clicks the info window of a
 * non-clustered item
 * @param onClusterItemInfoWindowLongClick a lambda invoked when the user long-clicks the info
 * window of a non-clustered item
 */
@MapsComposeExperimentalApi
@Composable
public fun <T : ClusterItem> Clustering(
    items: Collection<T>,
    cameraPositionState: CameraPositionState,
    onClusterClick: (Cluster<T>) -> Boolean = { false },
    onClusterItemClick: (T) -> Boolean = { false },
    onClusterItemInfoWindowClick: (T) -> Unit = { },
    onClusterItemInfoWindowLongClick: (T) -> Unit = { },
) {
    val clusterManager = rememberClusterManager<T>() ?: return

    ResetMapListeners(clusterManager)
    LaunchedEffect(
        clusterManager,
        onClusterClick,
        onClusterItemClick,
        onClusterItemInfoWindowClick,
        onClusterItemInfoWindowLongClick,
    ) {
        clusterManager.setOnClusterClickListener(onClusterClick)
        clusterManager.setOnClusterItemClickListener(onClusterItemClick)
        clusterManager.setOnClusterItemInfoWindowClickListener(onClusterItemInfoWindowClick)
        clusterManager.setOnClusterItemInfoWindowLongClickListener(onClusterItemInfoWindowLongClick)
    }
    InputHandler(
        onMarkerClick = clusterManager::onMarkerClick,
        onInfoWindowClick = clusterManager::onInfoWindowClick,
        onInfoWindowLongClick = clusterManager.markerManager::onInfoWindowLongClick,
        onMarkerDrag = clusterManager.markerManager::onMarkerDrag,
        onMarkerDragEnd = clusterManager.markerManager::onMarkerDragEnd,
        onMarkerDragStart = clusterManager.markerManager::onMarkerDragStart,
    )
    LaunchedEffect(cameraPositionState) {
        snapshotFlow { cameraPositionState.isMoving }
            .collect { isMoving ->
                if (!isMoving) {
                    clusterManager.onCameraIdle()
                }
            }
    }
    LaunchedEffect(items) {
        clusterManager.clearItems()
        clusterManager.addItems(items)
    }
}

@OptIn(MapsComposeExperimentalApi::class)
@Composable
private fun <T : ClusterItem> rememberClusterManager(): ClusterManager<T>? {
    val context = LocalContext.current
    var clusterManager: ClusterManager<T>? by remember { mutableStateOf(null) }
    MapEffect(context) { map ->
        clusterManager = ClusterManager<T>(context, map)
    }
    return clusterManager
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
