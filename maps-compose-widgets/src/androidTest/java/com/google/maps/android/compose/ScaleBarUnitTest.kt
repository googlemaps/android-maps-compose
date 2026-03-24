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

package com.google.maps.android.compose.widgets


import android.graphics.Point
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import com.google.common.truth.Truth.assertThat
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.ktx.utils.sphericalDistance
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
public class ScaleBarUnitTest {

    @Test
    public fun testScaleBarCalculation() {
        val projection = mockk<Projection>(relaxed = true)
        val density = Density(1f, 1f)
        val width = 100.dp

        val startPoint = Point(0, 0)
        val endPoint = Point(width.value.toInt(), 0)

        val startLatLng = LatLng(0.0, 0.0)
        val endLatLng = LatLng(0.0, 0.001)

        every { projection.fromScreenLocation(startPoint) } returns startLatLng
        every { projection.fromScreenLocation(endPoint) } returns endLatLng

        val expectedDistance = startLatLng.sphericalDistance(endLatLng)
        val expectedResult = (expectedDistance * 8 / 9).toInt()

        val result = calculateDistance(projection, width, density)

        assertThat(result).isEqualTo(expectedResult)
    }
}
