package com.google.maps.android.compose.clustering

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.View.MeasureSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.UiComposable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.applyCanvas
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.collections.MarkerManager
import com.google.maps.android.compose.ComposeUiViewRenderer
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.InputHandler
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.currentCameraPositionState
import com.google.maps.android.compose.rememberComposeUiViewRenderer
import com.google.maps.android.compose.rememberReattachClickListenersHandle
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
 * @param clusterContent an optional Composable that is rendered for each [Cluster]. This content is
 * static and cannot be animated.
 * @param clusterItemContent an optional Composable that is rendered for each non-clustered item.
 * This content is static and cannot be animated.
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
    val clusterManager = rememberClusterManager(clusterContent, clusterItemContent) ?: return

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
    val cameraPositionState = currentCameraPositionState
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
private fun <T : ClusterItem> rememberClusterManager(
    clusterContent: @Composable ((Cluster<T>) -> Unit)?,
    clusterItemContent: @Composable ((T) -> Unit)?,
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
                    val renderer = if (hasCustomContent) {
                        ComposeUiClusterRenderer(
                            context,
                            map,
                            clusterManager,
                            viewRendererState,
                            clusterContentState = clusterContentState,
                            clusterItemContentState = clusterItemContentState,
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

private class ComposeUiClusterRenderer <T : ClusterItem>(
    private val context: Context,
    map: GoogleMap,
    clusterManager: ClusterManager<T>,
    private val viewRendererState: State<ComposeUiViewRenderer>,
    private val clusterContentState: State<@Composable ((Cluster<T>) -> Unit)?>,
    private val clusterItemContentState: State<@Composable ((T) -> Unit)?>,
) : DefaultClusterRenderer<T>(
    context,
    map,
    clusterManager
) {

    private val maxMarkerSize = MaxMarkerSize.toAndroidSize(context)
    private val composeView = ComposeView(context)

    override fun getDescriptorForCluster(cluster: Cluster<T>): BitmapDescriptor {
        return if (clusterContentState.value != null) {
            composeView.setContent { clusterContentState.value?.invoke(cluster) }
            renderViewToBitmapDescriptor(composeView)
        } else {
            super.getDescriptorForCluster(cluster)
        }
    }

    override fun onBeforeClusterItemRendered(item: T, markerOptions: MarkerOptions) {
        super.onBeforeClusterItemRendered(item, markerOptions)

        if (clusterItemContentState.value != null) {
            composeView.setContent { clusterItemContentState.value?.invoke(item) }
            markerOptions.icon(renderViewToBitmapDescriptor(composeView))
        }
    }

    private fun renderViewToBitmapDescriptor(view: ComposeView): BitmapDescriptor {
        lateinit var bitmap: Bitmap // onAddedToWindow is called in place
        viewRendererState.value.renderView(
            view = view,
            onAddedToWindow = {
                view.measure(
                    MeasureSpec.makeMeasureSpec(maxMarkerSize.width, MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(maxMarkerSize.height, MeasureSpec.AT_MOST),
                )
                val actualSize = Size(
                    view.measuredWidth.coerceAtMost(maxMarkerSize.width),
                    view.measuredHeight.coerceAtMost(maxMarkerSize.height),
                )
                view.layout(0, 0, actualSize.width, actualSize.height)
                bitmap = Bitmap.createBitmap(
                    actualSize.width,
                    actualSize.height,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.applyCanvas {
                    view.draw(this)
                }
            }
        )

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    companion object {

        private val MaxMarkerSize = DpSize(width = 40.dp, height = 40.dp)

        private fun DpSize.toAndroidSize(context: Context): Size {
            val density = context.resources.displayMetrics.density
            return Size(
                (width.value * density).roundToInt(),
                (height.value * density).roundToInt()
            )
        }

    }

}
