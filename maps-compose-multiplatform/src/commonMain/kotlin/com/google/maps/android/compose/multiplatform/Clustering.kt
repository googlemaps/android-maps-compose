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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm
import com.google.maps.android.model.LatLng
import com.google.maps.android.model.latitude
import com.google.maps.android.model.longitude

/**
 * Adapts a [MapMarker] to android-maps-utils' [ClusterItem] so it can be fed to the
 * multiplatform clustering algorithms.
 */
private class MapMarkerClusterItem(
    val marker: MapMarker,
) : ClusterItem {
    override val position: LatLng = LatLng(marker.latitude, marker.longitude)

    override val title: String? = marker.title

    override val snippet: String? = marker.snippet

    override val zIndex: Float? = null
}

/**
 * Groups [markers] into clusters for the given [zoom] level using the multiplatform
 * clustering algorithm from android-maps-utils. Runs entirely in common code, so Android
 * and iOS produce identical clusters.
 *
 * Clusters of one item pass the original marker through; larger clusters are represented
 * by a marker at the cluster position whose title carries the cluster size.
 */
public fun clusterMarkers(
    markers: List<MapMarker>,
    zoom: Float,
    maxDistanceBetweenClusteredItems: Int = 100,
): List<MapMarker> {
    val algorithm = NonHierarchicalDistanceBasedAlgorithm<MapMarkerClusterItem>()
    algorithm.maxDistanceBetweenClusteredItems = maxDistanceBetweenClusteredItems
    algorithm.addItems(markers.map { MapMarkerClusterItem(it) })
    return algorithm.getClusters(zoom).map { cluster ->
        val items = cluster.items
        if (items.size == 1) {
            items.first().marker
        } else {
            MapMarker(
                latitude = cluster.position.latitude,
                longitude = cluster.position.longitude,
                title = "${items.size} items",
            )
        }
    }
}

/**
 * A [GoogleMap] that clusters [markers] with android-maps-utils' multiplatform clustering
 * algorithm before rendering them. Clustering is recomputed when the markers or [zoom]
 * change.
 */
@Composable
public fun ClusteredGoogleMap(
    modifier: Modifier = Modifier,
    latitude: Double,
    longitude: Double,
    zoom: Float = 10f,
    mapType: MapType = MapType.NORMAL,
    myLocationEnabled: Boolean = false,
    scrollGesturesEnabled: Boolean = true,
    zoomGesturesEnabled: Boolean = true,
    markers: List<MapMarker> = emptyList(),
    maxDistanceBetweenClusteredItems: Int = 100,
) {
    val clustered = remember(markers, zoom, maxDistanceBetweenClusteredItems) {
        clusterMarkers(markers, zoom, maxDistanceBetweenClusteredItems)
    }
    GoogleMap(
        modifier = modifier,
        latitude = latitude,
        longitude = longitude,
        zoom = zoom,
        mapType = mapType,
        myLocationEnabled = myLocationEnabled,
        scrollGesturesEnabled = scrollGesturesEnabled,
        zoomGesturesEnabled = zoomGesturesEnabled,
        markers = clustered,
    )
}
