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

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

internal class ScaleBarDistanceTest {
    private val originalLocale = Locale.getDefault()

    @Before
    fun setUp() {
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun metricDistancePreservesFractionalKilometers() {
        assertEquals("1.9 km", formatMetricDistance(1900))
        assertEquals("1.01 km", formatMetricDistance(1010))
    }

    @Test
    fun imperialDistancePreservesFractionalMiles() {
        assertEquals("1.24 mi", formatImperialDistance(2000))
        assertEquals("1.99 mi", formatImperialDistance(3200))
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
    fun fractionalDistancesUseLocaleDecimalSeparator() {
        Locale.setDefault(Locale.GERMANY)
        assertEquals("1,9 km", formatMetricDistance(1900))
        assertEquals("1,24 mi", formatImperialDistance(2000))
    }
}
