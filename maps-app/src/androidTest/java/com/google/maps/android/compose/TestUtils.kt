package com.google.maps.android.compose

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
const val timeout2 = 2_000L
const val timeout3 = 3_000L
const val timeout5 = 5_000L
const val MAP_LOAD_TIMEOUT_SECONDS = 30L

val hasValidApiKey: Boolean =
    BuildConfig.MAPS_API_KEY.isNotBlank() && BuildConfig.MAPS_API_KEY != "YOUR_API_KEY"

const val assertRoundingError: Double = 0.01

fun LatLng.assertEquals(other: LatLng) {
    assertEquals(latitude, other.latitude, assertRoundingError)
    assertEquals(longitude, other.longitude, assertRoundingError)
}


fun ComposeMapColorScheme.assertEquals(other: ComposeMapColorScheme) {
    assertEquals(other, this)
}