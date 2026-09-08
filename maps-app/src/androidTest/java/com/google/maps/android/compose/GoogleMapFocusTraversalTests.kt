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

package com.google.maps.android.compose

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import org.junit.Rule
import org.junit.Test

/**
 * Verifies keyboard focus traversal behavior for [GoogleMap] when integrated
 * inside Compose layouts, ensuring the map behaves as a single focus stop.
 */
class GoogleMapFocusTraversalTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mapItems = listOf(
        MapListItem(id = "1", location = LatLng(1.23, 4.56), zoom = 10f, title = "Item 1"),
        MapListItem(id = "2", location = LatLng(7.89, 0.12), zoom = 12f, title = "Item 2"),
        MapListItem(id = "3", location = LatLng(3.45, 6.78), zoom = 11f, title = "Item 3"),
    )

    private fun initMaps() {
        check(hasValidApiKey) { "Maps API key not specified" }

        composeTestRule.setContent {
            MapsInLazyColumn(
                mapItems,
                lazyListState = rememberLazyListState(),
                onMapLoaded = {},
            )
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithTag("Map", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .size >= 2
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun tabFromMapFocusesNextMap() {
        initMaps()

        val visibleMaps = composeTestRule.onAllNodesWithTag("Map", useUnmergedTree = true)
        visibleMaps[0].requestFocus()
        visibleMaps[0].assertIsFocused()

        visibleMaps[0].performKeyInput {
            pressKey(Key.Tab)
        }

        visibleMaps[1].assertIsFocused()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun nonFocusableMapIsSkippedDuringTabTraversal() {
        check(hasValidApiKey) { "Maps API key not specified" }

        composeTestRule.setContent {
            Column {
                // Plain focusable() elements are used instead of Buttons because
                // clickable-based components only accept focus in keyboard mode,
                // and the test starts in touch mode.
                Box(
                    Modifier
                        .testTag("Item1")
                        .size(48.dp)
                        .focusable()
                )
                GoogleMap(
                    modifier = Modifier
                        .testTag("Map")
                        .size(200.dp),
                    focusable = false,
                )
                Box(
                    Modifier
                        .testTag("Item2")
                        .size(48.dp)
                        .focusable()
                )
            }
        }

        // Focus traversal only requires the view hierarchy to be composed and attached,
        // not the map tiles to have loaded.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithTag("Map", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule.onNodeWithTag("Item1").requestFocus()
        composeTestRule.onNodeWithTag("Item1").assertIsFocused()

        composeTestRule.onNodeWithTag("Item1").performKeyInput {
            pressKey(Key.Tab)
        }

        composeTestRule.onNodeWithTag("Item2").assertIsFocused()
    }
}
