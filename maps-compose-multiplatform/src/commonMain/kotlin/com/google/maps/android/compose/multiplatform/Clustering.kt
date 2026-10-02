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

package com.google.maps.android.compose.multiplatform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm
import com.google.maps.android.model.zoom
import kotlin.math.floor
import kotlinx.coroutines.launch

/**
 * Groups [items] that are close together on screen into clusters, at the given [zoom].
 *
 * This is the algorithm [Clustering] uses, exposed for code that draws clusters itself.
 *
 * @param maxDistanceBetweenClusteredItems the largest distance, in density-independent pixels,
 * between items that share a cluster
 */
public fun <T : ClusterItem> clusterItems(
    items: Collection<T>,
    zoom: Float,
    maxDistanceBetweenClusteredItems: Int = DEFAULT_MAX_DISTANCE,
): List<Cluster<T>> {
    if (items.isEmpty()) return emptyList()
    val algorithm = NonHierarchicalDistanceBasedAlgorithm<T>()
    algorithm.maxDistanceBetweenClusteredItems = maxDistanceBetweenClusteredItems
    algorithm.addItems(items)
    return algorithm.getClusters(zoom).toList()
}

/**
 * Draws [items] as markers, grouping the ones close together on screen into a single cluster
 * marker. Clusters are recomputed as the camera zooms, once per whole zoom level.
 *
 * Must be called from the content of a [GoogleMap].
 *
 * @param onClusterClick called when a cluster marker is tapped. Return true to consume the tap;
 * otherwise the camera zooms in on the cluster.
 * @param onClusterItemClick called when the marker of a single item is tapped. Return true to
 * consume the tap and skip showing its info window.
 * @param clusterTitle the info window title of a cluster marker
 * @param clusterColor the pin color of cluster markers, to tell them apart from single items
 * @param maxDistanceBetweenClusteredItems the largest distance, in density-independent pixels,
 * between items that share a cluster
 */
@Composable
public fun <T : ClusterItem> Clustering(
    items: Collection<T>,
    onClusterClick: (Cluster<T>) -> Boolean = { false },
    onClusterItemClick: (T) -> Boolean = { false },
    clusterTitle: (Cluster<T>) -> String = { "${it.size} items" },
    clusterColor: Color = DefaultClusterColor,
    maxDistanceBetweenClusteredItems: Int = DEFAULT_MAX_DISTANCE,
) {
    val cameraPositionState = checkNotNull(LocalCameraPositionState.current) {
        "Clustering must be called from the content of a GoogleMap."
    }
    // The algorithm only distinguishes whole zoom levels, so skip the fractional changes of a
    // pinch or an animation.
    val zoomLevel by remember(cameraPositionState) {
        derivedStateOf { floor(cameraPositionState.position.zoom) }
    }
    val clusters = remember(items, zoomLevel, maxDistanceBetweenClusteredItems) {
        clusterItems(items, zoomLevel, maxDistanceBetweenClusteredItems)
    }
    val scope = rememberCoroutineScope()

    clusters.forEach { cluster ->
        if (cluster.size == 1) {
            val item = cluster.items.first()
            key(item) {
                Marker(
                    state = rememberMarkerState(item.position),
                    title = item.title,
                    snippet = item.snippet,
                    zIndex = item.zIndex ?: 0f,
                    onClick = { onClusterItemClick(item) },
                )
            }
        } else {
            // A cluster has no identity across zoom levels, so key it by its position and size.
            key(cluster.position, cluster.size) {
                Marker(
                    state = rememberMarkerState(cluster.position),
                    title = clusterTitle(cluster),
                    color = clusterColor,
                    onClick = {
                        if (!onClusterClick(cluster)) {
                            scope.launch {
                                cameraPositionState.animate(
                                    cameraPosition(cluster.position, zoomLevel + CLUSTER_ZOOM_STEP),
                                )
                            }
                        }
                        true
                    },
                )
            }
        }
    }
}

private const val DEFAULT_MAX_DISTANCE = 100

private val DefaultClusterColor = Color(0xFF1A73E8)

/** How far a tap on a cluster zooms in: enough to split most clusters. */
private const val CLUSTER_ZOOM_STEP = 2f
