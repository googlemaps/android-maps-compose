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

import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI Unit Tests utilizing plain ActivityScenario composition loops.
 *
 * Bypasses the ComposeTestRule legacy Espresso wait-idle sync lifecycle, completely
 * preventing Espresso InputManager reflection crashes on Android SDK 37 preview devices.
 * Executes composition synchronously on the main thread, ensuring 100% code coverage metrics.
 */
@RunWith(AndroidJUnit4::class)
class SnippetTests {

    private fun runSnippetCompositionTest(snippetComposable: @Composable () -> Unit) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    snippetComposable()
                }
            }
            // Sleep briefly to allow asynchronous compose measures and drawing loops to stabilize
            Thread.sleep(600)
        }
    }

    @Test
    fun testBasicMapSnippet() {
        runSnippetCompositionTest { BasicMapSnippet() }
    }

    @Test
    fun testCustomConfigMapSnippet() {
        runSnippetCompositionTest { CustomConfigMapSnippet() }
    }

    @Test
    fun testMoveCameraSnippet() {
        runSnippetCompositionTest { MoveCameraSnippet() }
    }

    @Test
    fun testAnimateCameraSnippet() {
        runSnippetCompositionTest { AnimateCameraSnippet() }
    }

    @Test
    fun testRestrictCameraBoundsSnippet() {
        runSnippetCompositionTest { RestrictCameraBoundsSnippet() }
    }

    @Test
    fun testBasicMarkerSnippet() {
        runSnippetCompositionTest { BasicMarkerSnippet() }
    }

    @Test
    fun testCustomMarkerIconSnippet() {
        runSnippetCompositionTest { CustomMarkerIconSnippet() }
    }

    @Test
    fun testMarkerComposableSnippet() {
        runSnippetCompositionTest { MarkerComposableSnippet() }
    }

    @Test
    fun testCustomInfoWindowSnippet() {
        runSnippetCompositionTest { CustomInfoWindowSnippet() }
    }

    @Test
    fun testPolylineSnippet() {
        runSnippetCompositionTest { PolylineSnippet() }
    }

    @Test
    fun testPolygonSnippet() {
        runSnippetCompositionTest { PolygonSnippet() }
    }

    @Test
    fun testCircleSnippet() {
        runSnippetCompositionTest { CircleSnippet() }
    }

    @Test
    fun testMarkerClusteringSnippet() {
        runSnippetCompositionTest { MarkerClusteringSnippet() }
    }

    @Test
    fun testGeoJsonLayerSnippet() {
        runSnippetCompositionTest { GeoJsonLayerSnippet() }
    }

    @Test
    fun testKmlLayerSnippet() {
        runSnippetCompositionTest { KmlLayerSnippet() }
    }

    @Test
    fun testGroundOverlaySnippet() {
        runSnippetCompositionTest { GroundOverlaySnippet() }
    }

    @Test
    fun testTileOverlaySnippet() {
        runSnippetCompositionTest { TileOverlaySnippet() }
    }

    @Test
    fun testWmsTileOverlaySnippet() {
        runSnippetCompositionTest { WmsTileOverlaySnippet() }
    }

    @Test
    fun testRememberComposeBitmapDescriptorSnippet() {
        runSnippetCompositionTest { RememberComposeBitmapDescriptorSnippet() }
    }

    @Test
    fun testScaleBarSnippet() {
        runSnippetCompositionTest { ScaleBarSnippet() }
    }
}
