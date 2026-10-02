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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Runs on the JVM and on the iOS simulator, so it also checks that the multiplatform clustering
 * from android-maps-utils behaves the same on both.
 */
class ClusterMarkersTest {
    private val goldenGate = MapMarker(37.8199, -122.4783, title = "Golden Gate")
    private val alcatraz = MapMarker(37.8267, -122.4230, title = "Alcatraz")
    private val coitTower = MapMarker(37.8024, -122.4058, title = "Coit Tower")
    private val london = MapMarker(51.5007, -0.1246, title = "Big Ben")

    @Test
    fun noMarkersGiveNoClusters() {
        assertTrue(clusterMarkers(emptyList(), zoom = 10f).isEmpty())
    }

    @Test
    fun aSingleMarkerIsReturnedUnchanged() {
        val result = clusterMarkers(listOf(goldenGate), zoom = 10f)

        assertSame(goldenGate, result.single())
    }

    @Test
    fun nearbyMarkersMergeWhenZoomedOut() {
        val result = clusterMarkers(listOf(goldenGate, alcatraz, coitTower), zoom = 5f)

        val cluster = result.single()
        assertEquals("3 items", cluster.title)
    }

    @Test
    fun nearbyMarkersSplitWhenZoomedIn() {
        val markers = listOf(goldenGate, alcatraz, coitTower)

        val result = clusterMarkers(markers, zoom = 18f)

        assertEquals(markers.toSet(), result.toSet())
    }

    @Test
    fun distantMarkersNeverMerge() {
        val result = clusterMarkers(listOf(goldenGate, london), zoom = 3f)

        assertEquals(setOf(goldenGate, london), result.toSet())
    }

    @Test
    fun aLargerDistanceMergesMoreMarkers() {
        val markers = listOf(goldenGate, alcatraz, coitTower)

        val tight = clusterMarkers(markers, zoom = 12f, maxDistanceBetweenClusteredItems = 1)
        val loose = clusterMarkers(markers, zoom = 12f, maxDistanceBetweenClusteredItems = 1_000)

        assertEquals(3, tight.size)
        assertEquals(1, loose.size)
    }

    @Test
    fun aClusterIsPlacedAtOneOfItsMarkers() {
        val markers = listOf(goldenGate, alcatraz, coitTower)

        val cluster = clusterMarkers(markers, zoom = 5f).single()

        assertTrue(markers.any { it.latitude == cluster.latitude && it.longitude == cluster.longitude })
    }
}
