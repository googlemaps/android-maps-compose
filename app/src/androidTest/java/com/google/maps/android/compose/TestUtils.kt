package com.google.maps.android.compose

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals

const val assertRoundingError: Double = 0.01

fun LatLng.assertEquals(other: LatLng) {
    assertEquals(latitude, other.latitude, assertRoundingError)
    assertEquals(longitude, other.longitude, assertRoundingError)
}