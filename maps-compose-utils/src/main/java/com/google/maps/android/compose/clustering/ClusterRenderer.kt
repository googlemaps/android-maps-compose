package com.google.maps.android.compose.clustering

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.platform.AbstractComposeView
import androidx.core.graphics.applyCanvas
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.ClusterRenderer
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.compose.ComposeUiViewRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Implementation of [ClusterRenderer] that renders marker bitmaps from Compose UI content.
 * [clusterContentState] renders clusters, and [clusterItemContentState] renders non-clustered
 * items.
 */
internal class ComposeUiClusterRenderer<T : ClusterItem>(
    private val context: Context,
    private val scope: CoroutineScope,
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

    private val fakeCanvas = Canvas()
    private val keysToViews = mutableMapOf<ViewKey<T>, ViewInfo>()

    override fun onClustersChanged(clusters: Set<Cluster<T>>) {
        super.onClustersChanged(clusters)
        val keys = clusters.flatMap { it.computeViewKeys() }

        with(keysToViews.iterator()) {
            forEach { (key, viewInfo) ->
                if (key !in keys) {
                    remove()
                    viewInfo.onRemove()
                }
            }
        }
        keys.forEach { key ->
            if (key !in keysToViews.keys) {
                createAndAddView(key)
            }
        }
    }

    /**
     * A [Cluster] is represented by one or more elements on screen. Even if a cluster contains
     * multiple items, it still might only need a single element, depending on
     * [shouldRenderAsCluster].
     * @return a set of [ViewKey]s for each element.
     */
    private fun Cluster<T>.computeViewKeys(): Set<ViewKey<T>> {
        return if (shouldRenderAsCluster(this)) {
            setOf(ViewKey.Cluster(this))
        } else {
            items.mapTo(mutableSetOf()) { ViewKey.Item(it) }
        }
    }

    private fun createAndAddView(key: ViewKey<T>): ViewInfo {
        val view = InvalidatingComposeView(
            context,
            content = when (key) {
                is ViewKey.Cluster -> {
                    { clusterContentState.value?.invoke(key.cluster) }
                }

                is ViewKey.Item -> {
                    { clusterItemContentState.value?.invoke(key.item) }
                }
            }
        )
        val renderHandle = viewRendererState.value.startRenderingView(view)
        val rerenderJob = scope.launch {
            collectInvalidationsAndRerender(key, view)
        }

        val viewInfo = ViewInfo(
            view,
            onRemove = {
                rerenderJob.cancel()
                renderHandle.dispose()
            },
        )
        keysToViews[key] = viewInfo
        return viewInfo
    }

    /** Re-render the corresponding marker whenever [view] invalidates */
    private suspend fun collectInvalidationsAndRerender(
        key: ViewKey<T>,
        view: InvalidatingComposeView
    ) {
        callbackFlow {
            // When invalidated, emit on the next frame
            var invalidated = false
            view.onInvalidate = {
                if (!invalidated) {
                    launch {
                        awaitFrame()
                        trySend(Unit)
                        invalidated = false
                    }
                    invalidated = true
                }
            }
            view.doOnAttach {
                view.doOnDetach { close() }
            }
            awaitClose()
        }
            .collectLatest {
                when (key) {
                    is ViewKey.Cluster -> getMarker(key.cluster)
                    is ViewKey.Item -> getMarker(key.item)
                }?.setIcon(renderViewToBitmapDescriptor(view))
            }

    }

    override fun getDescriptorForCluster(cluster: Cluster<T>): BitmapDescriptor {
        return if (clusterContentState.value != null) {
            val viewInfo = keysToViews.entries
                .firstOrNull { (key, _) -> (key as? ViewKey.Cluster)?.cluster == cluster }
                ?.value
                ?: createAndAddView(cluster.computeViewKeys().first())
            renderViewToBitmapDescriptor(viewInfo.view)
        } else {
            super.getDescriptorForCluster(cluster)
        }
    }

    override fun onBeforeClusterItemRendered(item: T, markerOptions: MarkerOptions) {
        super.onBeforeClusterItemRendered(item, markerOptions)

        if (clusterItemContentState.value != null) {
            val viewInfo = keysToViews.entries
                .firstOrNull { (key, _) -> (key as? ViewKey.Item)?.item == item }
                ?.value
                ?: createAndAddView(ViewKey.Item(item))
            markerOptions.icon(renderViewToBitmapDescriptor(viewInfo.view))
        }
    }

    private fun renderViewToBitmapDescriptor(view: AbstractComposeView): BitmapDescriptor {
        /* AndroidComposeView triggers LayoutNode's layout phase in the View draw phase,
           so trigger a draw to an empty canvas to force that */
        view.draw(fakeCanvas)
        val viewParent = (view.parent as ViewGroup)
        view.measure(
            View.MeasureSpec.makeMeasureSpec(viewParent.width, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(viewParent.height, View.MeasureSpec.AT_MOST),
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        val bitmap = Bitmap.createBitmap(
            view.measuredWidth,
            view.measuredHeight,
            Bitmap.Config.ARGB_8888
        )
        bitmap.applyCanvas {
            view.draw(this)
        }

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private sealed class ViewKey<T : ClusterItem> {
        data class Cluster<T : ClusterItem>(
            val cluster: com.google.maps.android.clustering.Cluster<T>
        ) : ViewKey<T>()

        data class Item<T : ClusterItem>(
            val item: T
        ) : ViewKey<T>()
    }

    private class ViewInfo(
        val view: AbstractComposeView,
        val onRemove: () -> Unit,
    )

    /**
     * An [AbstractComposeView] that calls [onInvalidate] whenever the Compose render layer is
     * invalidated. Works by reporting invalidations from its inner AndroidComposeView.
     */
    private class InvalidatingComposeView(
        context: Context,
        private val content: @Composable () -> Unit,
    ) : AbstractComposeView(context) {

        var onInvalidate: (() -> Unit)? = null

        @Composable
        override fun Content() = content()

        override fun onDescendantInvalidated(child: View, target: View) {
            super.onDescendantInvalidated(child, target)
            onInvalidate?.invoke()
        }
    }

}
