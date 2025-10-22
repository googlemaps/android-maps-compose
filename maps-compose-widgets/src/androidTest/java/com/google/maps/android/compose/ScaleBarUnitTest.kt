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
