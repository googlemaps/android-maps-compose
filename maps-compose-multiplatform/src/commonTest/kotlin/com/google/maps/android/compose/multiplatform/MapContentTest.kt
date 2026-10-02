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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.google.maps.android.model.LatLng
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MapContentTest {
    private val sanFrancisco = LatLng(37.7749, -122.4194)
    private val oakland = LatLng(37.8044, -122.2712)

    @Test
    fun aMarkerRegistersOneNodeWithItsProperties() {
        val state = MarkerState(sanFrancisco)
        runMapContentTest(content = {
            Marker(state = state, title = "SF", snippet = "Welcome", draggable = true, zIndex = 2f)
        }) {
            val node = nodes.single() as MarkerNode
            assertSame(state, node.state)
            assertEquals("SF", node.title)
            assertEquals("Welcome", node.snippet)
            assertTrue(node.draggable)
            assertEquals(2f, node.zIndex)
        }
    }

    @Test
    fun changingAPropertyUpdatesTheSameNode() {
        var title by mutableStateOf("Before")
        runMapContentTest(content = {
            Marker(state = rememberMarkerState(sanFrancisco), title = title)
        }) {
            val node = nodes.single() as MarkerNode

            title = "After"
            awaitIdle()

            assertSame(node, nodes.single())
            assertEquals("After", node.title)
        }
    }

    @Test
    fun contentLeavingTheCompositionUnregistersItsNode() {
        var showCircle by mutableStateOf(true)
        runMapContentTest(content = {
            Marker(state = rememberMarkerState(sanFrancisco))
            if (showCircle) Circle(center = oakland, radius = 1_000.0)
        }) {
            assertEquals(2, nodes.size)

            showCircle = false
            awaitIdle()

            assertTrue(nodes.single() is MarkerNode)
        }
    }

    @Test
    fun shapesRegisterTheirGeometryAndStyle() {
        val points = listOf(sanFrancisco, oakland)
        runMapContentTest(content = {
            Polyline(points = points, color = Color.Red, width = 4f, clickable = true)
            Polygon(points = points + LatLng(37.7, -122.3), fillColor = Color.Blue)
            Circle(center = oakland, radius = 500.0, strokeColor = Color.Green)
        }) {
            val polyline = nodes[0] as PolylineNode
            val polygon = nodes[1] as PolygonNode
            val circle = nodes[2] as CircleNode
            assertEquals(points, polyline.points)
            assertEquals(Color.Red, polyline.color)
            assertTrue(polyline.clickable)
            assertEquals(3, polygon.points.size)
            assertEquals(Color.Blue, polygon.fillColor)
            assertEquals(oakland, circle.center)
            assertEquals(500.0, circle.radius)
            assertEquals(Color.Green, circle.strokeColor)
        }
    }

    @Test
    fun aMarkerClickReachesItsCallbackWithItsState() {
        val state = MarkerState(sanFrancisco)
        var clicked: MarkerState? = null
        runMapContentTest(content = {
            Marker(state = state, onClick = { clicked = it; true })
        }) {
            val node = nodes.single() as MarkerNode

            val consumed = node.onClick(node.state)

            assertTrue(consumed)
            assertSame(state, clicked)
        }
    }
}
