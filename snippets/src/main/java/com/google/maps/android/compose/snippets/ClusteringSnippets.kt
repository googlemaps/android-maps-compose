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

package com.google.maps.android.compose.snippets

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.rememberCameraPositionState
import kotlin.OptIn
import com.google.maps.android.compose.MapsComposeExperimentalApi

/**
 * A lightweight representation of a marker cluster item mapped for testing.
 *
 * Implements [ClusterItem] to supply standard coordinates, title labels, and descriptions
 * required by the grouping algorithms inside maps utility libraries.
 */
data class SimpleClusterItem(
    private val position: LatLng,
    private val title: String,
    private val snippet: String
) : ClusterItem {
    override fun getPosition(): LatLng = position
    override fun getTitle(): String = title
    override fun getSnippet(): String = snippet
    override fun getZIndex(): Float? = null
}

/**
 * Demonstrates how to group multiple adjacent markers dynamically inside clusters.
 *
 * This snippet leverages the Compose utility extension composable [Clustering] inside the map content
 * block. It manages clustering animations, rendering, and click events automatically on zoom adjustments,
 * preventing map interface clutter.
 */
@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun MarkerClusteringSnippet() {
    // [START maps_android_compose_clustering]
    val cameraPositionState = rememberCameraPositionState {
        position = defaultCameraPosition
    }

    // List of items to be clustered on the map
    val clusterItems = remember {
        listOf(
            SimpleClusterItem(LatLng(1.35, 103.87), "Marker 1", "Snippet 1"),
            SimpleClusterItem(LatLng(1.36, 103.88), "Marker 2", "Snippet 2"),
            SimpleClusterItem(LatLng(1.37, 103.89), "Marker 3", "Snippet 3"),
            SimpleClusterItem(LatLng(1.38, 103.90), "Marker 4", "Snippet 4"),
        )
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        // Clustering utility composable manages marker layout automatically
        Clustering(
            items = clusterItems,
            onClusterItemClick = { item ->
                // Handle individual item click
                false
            },
            onClusterClick = { cluster ->
                // Handle cluster group click
                false
            }
        )
    }
    // [END maps_android_compose_clustering]
}
