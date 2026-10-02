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

import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.model.LatLng
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Runs on the JVM and on the iOS simulator, so it also checks that the multiplatform clustering
 * from android-maps-utils behaves the same on both.
 */
class ClusteringTest {
    private class Place(
        override val title: String,
        latitude: Double,
        longitude: Double,
    ) : ClusterItem {
        override val position: LatLng = LatLng(latitude, longitude)
        override val snippet: String? = null
        override val zIndex: Float? = null
    }

    private val goldenGate = Place("Golden Gate", 37.8199, -122.4783)
    private val alcatraz = Place("Alcatraz", 37.8267, -122.4230)
    private val coitTower = Place("Coit Tower", 37.8024, -122.4058)
    private val bigBen = Place("Big Ben", 51.5007, -0.1246)
    private val sanFrancisco = listOf(goldenGate, alcatraz, coitTower)

    @Test
    fun noItemsGiveNoClusters() {
        assertTrue(clusterItems(emptyList<Place>(), zoom = 10f).isEmpty())
    }

    @Test
    fun nearbyItemsShareAClusterWhenZoomedOut() {
        val cluster = clusterItems(sanFrancisco, zoom = 5f).single()

        assertEquals(sanFrancisco.toSet(), cluster.items.toSet())
    }

    @Test
    fun nearbyItemsSplitWhenZoomedIn() {
        val clusters = clusterItems(sanFrancisco, zoom = 18f)

        assertEquals(3, clusters.size)
        assertTrue(clusters.all { it.size == 1 })
    }

    @Test
    fun distantItemsNeverShareACluster() {
        assertEquals(2, clusterItems(listOf(goldenGate, bigBen), zoom = 3f).size)
    }

    @Test
    fun aLargerDistanceMergesMoreItems() {
        assertEquals(3, clusterItems(sanFrancisco, zoom = 12f, maxDistanceBetweenClusteredItems = 1).size)
        assertEquals(1, clusterItems(sanFrancisco, zoom = 12f, maxDistanceBetweenClusteredItems = 1_000).size)
    }

    @Test
    fun clusteringDrawsOneMarkerPerCluster() {
        val camera = CameraPositionState(cameraPosition(goldenGate.position, zoom = 5f))
        runMapContentTest(camera, content = { Clustering(sanFrancisco) }) {
            val marker = nodes.single() as MarkerNode
            assertEquals("3 items", marker.title)
        }
    }

    @Test
    fun clusteringSplitsWhenTheCameraZoomsIn() {
        val camera = CameraPositionState(cameraPosition(goldenGate.position, zoom = 5f))
        runMapContentTest(camera, content = { Clustering(sanFrancisco) }) {
            assertEquals(1, nodes.size)

            // As the platform map reports a gesture.
            camera.onCameraChanged(cameraPosition(goldenGate.position, zoom = 18f), isMoving = false)
            awaitIdle()

            val titles = nodes.map { (it as MarkerNode).title }.toSet()
            assertEquals(setOf("Golden Gate", "Alcatraz", "Coit Tower"), titles)
        }
    }

    @Test
    fun clusteringUsesTheGivenClusterTitle() {
        val camera = CameraPositionState(cameraPosition(goldenGate.position, zoom = 5f))
        runMapContentTest(camera, content = {
            Clustering(sanFrancisco, clusterTitle = { "${it.size} places" })
        }) {
            assertEquals("3 places", (nodes.single() as MarkerNode).title)
        }
    }
}
