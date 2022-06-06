// Copyright 2022 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.maps.android.compose

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val TAG = "MapInColumnTests"

class MapInColumnTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val startingZoom = 10f
    private val startingPosition = LatLng(1.23, 4.56)
    private lateinit var cameraPositionState: CameraPositionState

    private fun initMap(content: @Composable () -> Unit = {}) {
        val countDownLatch = CountDownLatch(1)
        composeTestRule.setContent {
            var scrollingEnabled by remember { mutableStateOf(true) }

            LaunchedEffect(cameraPositionState.isMoving) {
                if (!cameraPositionState.isMoving) {
                    scrollingEnabled = true
                    Log.d(TAG, "Map camera stopped moving - Enabling column scrolling...")
                }
            }

            MapInColumn(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState,
                columnScrollingEnabled = scrollingEnabled,
                onMapTouched = {
                    scrollingEnabled = false
                    Log.d(
                        TAG,
                        "User touched map - Disabling column scrolling after user touched this Box..."
                    )
                },
                onMapLoaded = {
                    countDownLatch.countDown()
                }
            )
        }
        val mapLoaded = countDownLatch.await(30, TimeUnit.SECONDS)
        assertTrue("Map loaded", mapLoaded)
    }

    @Before
    fun setUp() {
        cameraPositionState = CameraPositionState(
            position = CameraPosition.fromLatLngZoom(
                startingPosition,
                startingZoom
            )
        )
    }

    @Test
    fun testStartingCameraPosition() {
        initMap()
        startingPosition.assertEquals(cameraPositionState.position.target)
    }

    @Test
    fun testLatLngInVisibleRegion() {
        initMap()
        composeTestRule.runOnUiThread {
            val projection = cameraPositionState.projection
            assertNotNull(projection)
            assertTrue(
                projection!!.visibleRegion.latLngBounds.contains(startingPosition)
            )
        }
    }

    @Test
    fun testLatLngNotInVisibleRegion() {
        initMap()
        composeTestRule.runOnUiThread {
            val projection = cameraPositionState.projection
            assertNotNull(projection)
            val latLng = LatLng(23.4, 25.6)
            assertFalse(
                projection!!.visibleRegion.latLngBounds.contains(latLng)
            )
        }
    }

    @Test
    fun testScrollColumn_MapCameraRemainsSame() {
        initMap()
        // Check that the column scrolls to the last item
        composeTestRule.onRoot().performTouchInput { swipeUp() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("Item 1").assertIsNotDisplayed()

        // Check that the map didn't change
        startingPosition.assertEquals(cameraPositionState.position.target)
    }

//    @Test
//    fun testPanMapUp_MapCameraChangesColumnDoesNotScroll() {
//        initMap()
//        // Swipe the map up
//        // FIXME - for some reason this scrolls the entire column instead of just the map
//        composeTestRule.onNodeWithTag("Map").performTouchInput { swipeUp() }
//        composeTestRule.waitForIdle()
//
//        // Make sure that the map changed (i.e., we can scroll the map in the column)
//        startingPosition.assertNotEquals(cameraPositionState.position.target)
//
//        // Check to make sure column didn't scroll
//        composeTestRule.onNodeWithTag("Item 1").assertIsDisplayed()
//    }
}