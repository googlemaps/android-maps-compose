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

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield

/**
 * Composes map content without a platform map, the way [GoogleMap] does, so that tests can
 * check which nodes the content registers. Runs on every target.
 */
internal class MapContentTestHarness(
    private val clock: BroadcastFrameClock,
    val cameraPositionState: CameraPositionState,
) {
    val registry = MapNodeRegistry()
    private var frameTime = 0L

    val nodes: List<MapNode> get() = registry.nodes.toList()

    fun setContent(composition: Composition, content: @Composable () -> Unit) {
        composition.setContent {
            CompositionLocalProvider(
                LocalMapNodeRegistry provides registry,
                LocalCameraPositionState provides cameraPositionState,
            ) {
                content()
            }
        }
    }

    /** Applies pending state changes and lets the recomposer run a frame. */
    suspend fun awaitIdle() {
        repeat(3) {
            Snapshot.sendApplyNotifications()
            frameTime += 16_000_000L
            clock.sendFrame(frameTime)
            yield()
        }
    }
}

private class UnitApplier : AbstractApplier<Unit>(Unit) {
    override fun insertBottomUp(index: Int, instance: Unit) = Unit
    override fun insertTopDown(index: Int, instance: Unit) = Unit
    override fun move(from: Int, to: Int, count: Int) = Unit
    override fun remove(index: Int, count: Int) = Unit
    override fun onClear() = Unit
}

internal fun runMapContentTest(
    cameraPositionState: CameraPositionState = CameraPositionState(),
    content: @Composable () -> Unit,
    block: suspend MapContentTestHarness.() -> Unit,
) = runTest {
    val clock = BroadcastFrameClock()
    val recomposer = Recomposer(coroutineContext + clock)
    val recomposeJob = launch(clock) { recomposer.runRecomposeAndApplyChanges() }
    val composition = Composition(UnitApplier(), recomposer)
    val harness = MapContentTestHarness(clock, cameraPositionState)
    try {
        harness.setContent(composition, content)
        harness.awaitIdle()
        harness.block()
    } finally {
        composition.dispose()
        recomposer.cancel()
        recomposeJob.cancel()
    }
}
