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

package com.google.maps.android.compose.wms

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.google.android.gms.maps.GoogleMap
import com.google.android.maps.robolectric.annotation.EnableMapsShadows
import com.google.android.maps.robolectric.shadows.ShadowGoogleMap
import com.google.android.maps.robolectric.shadows.ShadowTileOverlay
import com.google.common.truth.Truth.assertThat
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.TileOverlayState
import com.google.maps.android.compose.rememberTileOverlayState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow

// [START maps_compose_utils_wms_tile_overlay_test]
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@EnableMapsShadows
@OptIn(MapsComposeExperimentalApi::class)
internal class WmsTileOverlayTest {

    @get:Rule
    internal val composeTestRule: AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity> =
        createAndroidComposeRule<ComponentActivity>()

    @Test
    internal fun testWmsTileOverlay_rendersAndAppliesProperties(): Unit {
        var underlyingMap: GoogleMap? = null
        lateinit var overlayState: TileOverlayState

        composeTestRule.setContent {
            overlayState = rememberTileOverlayState()
            GoogleMap {
                MapEffect(Unit) { map ->
                    underlyingMap = map
                }
                WmsTileOverlay(
                    urlFormatter = { xMin, yMin, xMax, yMax, zoom ->
                        "https://example.com/tiles?bbox=$xMin,$yMin,$xMax,$yMax&zoom=$zoom"
                    },
                    state = overlayState,
                    transparency = 0.5f,
                    fadeIn = false,
                    visible = true,
                    zIndex = 2.0f
                )
            }
        }

        composeTestRule.waitForIdle()

        assertThat(underlyingMap).isNotNull()
        val shadowMap = Shadow.extract(underlyingMap) as ShadowGoogleMap
        assertThat(shadowMap.tileOverlays).hasSize(1)

        val tileOverlay = shadowMap.tileOverlays.first()
        val shadowOverlay = Shadow.extract(tileOverlay) as ShadowTileOverlay

        assertThat(tileOverlay.transparency).isEqualTo(0.5f)
        assertThat(tileOverlay.fadeIn).isFalse()
        assertThat(tileOverlay.isVisible).isTrue()
        assertThat(tileOverlay.zIndex).isEqualTo(2.0f)

        // Verify clearTileCache via state
        overlayState.clearTileCache()
        assertThat(shadowOverlay.isTileCacheCleared).isTrue()
    }
}
// [END maps_compose_utils_wms_tile_overlay_test]
