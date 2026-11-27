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

package com.google.maps.android.compose.demo

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ComposeOverlaySyncActivityTest {

    @Test
    fun singleSampleReturnsZeroHz() {
        val meter = RollingRateMeter(windowNanos = 500_000_000L)

        assertThat(meter.record(1_000_000_000L)).isEqualTo(0f)
    }

    @Test
    fun measuresSixtyHzCallbacksAccurately() {
        val meter = RollingRateMeter(windowNanos = 500_000_000L)
        val stepNanos = 1_000_000_000L / 60L
        var hz = 0f

        for (i in 0 until 20) {
            hz = meter.record(i * stepNanos)
        }

        assertThat(hz).isWithin(1.0f).of(60f)
    }

    @Test
    fun measuresOneTwentyHzFramesAccurately() {
        val meter = RollingRateMeter(windowNanos = 500_000_000L)
        val stepNanos = 1_000_000_000L / 120L
        var hz = 0f

        for (i in 0 until 40) {
            hz = meter.record(i * stepNanos)
        }

        assertThat(hz).isWithin(1.5f).of(120f)
    }

    @Test
    fun prunesSamplesOutsideSlidingWindow() {
        val meter = RollingRateMeter(windowNanos = 200_000_000L)

        // Initial burst at 120 Hz.
        val fastStep = 1_000_000_000L / 120L
        for (i in 0 until 15) {
            meter.record(i * fastStep)
        }

        // Later burst at 60 Hz well past the 200 ms window.
        val slowStart = 1_000_000_000L
        val slowStep = 1_000_000_000L / 60L
        var hz = 0f
        for (i in 0 until 10) {
            hz = meter.record(slowStart + i * slowStep)
        }

        assertThat(hz).isWithin(1.0f).of(60f)
    }

    @Test
    fun resetClearsRecordedSamples() {
        val meter = RollingRateMeter(windowNanos = 500_000_000L)
        meter.record(0L)
        meter.record(16_666_666L)

        meter.reset()

        assertThat(meter.record(100_000_000L)).isEqualTo(0f)
    }
}
