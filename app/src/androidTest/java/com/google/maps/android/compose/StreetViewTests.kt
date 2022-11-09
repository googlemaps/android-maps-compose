package com.google.maps.android.compose

import androidx.compose.ui.test.junit4.createComposeRule
import com.google.android.gms.maps.StreetViewPanoramaOptions
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.streetview.StreetView
import com.google.maps.android.compose.streetview.StreetViewCameraPositionState
import com.google.maps.android.ktx.MapsExperimentalFeature
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class StreetViewTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(MapsExperimentalFeature::class)
    @Test
    fun testStartingStreetViewPosition() {
        val cameraPositionState = StreetViewCameraPositionState()
        val initialLatLng = singapore
        composeTestRule.setContent {
            StreetView(
                cameraPositionState = cameraPositionState,
                streetViewPanoramaOptionsFactory = {
                    StreetViewPanoramaOptions()
                        .position(initialLatLng)
                }
            )
        }
        composeTestRule.waitUntil(8000) {
            cameraPositionState.location.position.latitude != 0.0 &&
                cameraPositionState.location.position.longitude != 0.0
        }
        initialLatLng.assertEquals(cameraPositionState.location.position)
    }

    @Test
    fun testStreetViewOnClick() {
        TODO()
    }

    @Test
    fun testStreetViewOnLongClick() {
        TODO()
    }
}