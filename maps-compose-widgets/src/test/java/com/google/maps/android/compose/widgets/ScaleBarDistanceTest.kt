// Copyright 2026 Google LLC
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

package com.google.maps.android.compose.widgets

import org.junit.Assert.assertEquals
import org.junit.Test

internal class ScaleBarDistanceTest {
    @Test
    fun metricDistancePreservesFractionalKilometers() {
        assertEquals("1.9 km", formatMetricDistance(1900))
        assertEquals("1 km", formatMetricDistance(1010))
    }

    @Test
    fun imperialDistancePreservesFractionalMiles() {
        assertEquals("1.2 mi", formatImperialDistance(2000))
        assertEquals("2 mi", formatImperialDistance(3200))
    }

    @Test
    fun metricDistanceUsesKilometersAtUnitBoundary() {
        assertEquals("999 m", formatMetricDistance(999))
        assertEquals("1 km", formatMetricDistance(1000))
        assertEquals("2 km", formatMetricDistance(2000))
    }

    @Test
    fun smallDistancesKeepWholeMetersAndFeet() {
        assertEquals("0 m", formatMetricDistance(0))
        assertEquals("0 ft", formatImperialDistance(0))
        assertEquals("100 m", formatMetricDistance(100))
        assertEquals("328 ft", formatImperialDistance(100))
    }

    @Test
    fun largeDistancesRoundToWholeUnits() {
        assertEquals("10 km", formatMetricDistance(10_400))
        assertEquals("12 km", formatMetricDistance(11_600))
        assertEquals("10 mi", formatImperialDistance(16_100))
        assertEquals("3457 km", formatMetricDistance(3_456_780))
    }

    @Test
    fun fractionalDistancesTrimZerosAndRoundAtBoundaries() {
        assertEquals("1 km", formatMetricDistance(1049))
        assertEquals("1.1 km", formatMetricDistance(1050))
        assertEquals("9.9 km", formatMetricDistance(9949))
        assertEquals("10 km", formatMetricDistance(9950))
        assertEquals("10 km", formatMetricDistance(9999))
        assertEquals("10 km", formatMetricDistance(9990))
        assertEquals("3 ft", formatImperialDistance(1))
        assertEquals("7 ft", formatImperialDistance(2))
    }
}
