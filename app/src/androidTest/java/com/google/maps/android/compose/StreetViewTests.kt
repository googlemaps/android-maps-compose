package com.google.maps.android.compose

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.android.gms.maps.StreetViewPanoramaOptions
import com.google.android.gms.maps.model.StreetViewPanoramaOrientation
import com.google.maps.android.compose.streetview.StreetView
import com.google.maps.android.compose.streetview.StreetViewCameraPositionState
import com.google.maps.android.ktx.MapsExperimentalFeature
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class StreetViewTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var cameraPositionState: StreetViewCameraPositionState
    private val initialLatLng = singapore

    @Before
    fun setUp() {
        cameraPositionState = StreetViewCameraPositionState()
    }

    @OptIn(MapsExperimentalFeature::class)
    private fun initStreetView(onClick: (StreetViewPanoramaOrientation) -> Unit = {}) {
        composeTestRule.setContent {
            StreetView(
                Modifier.semantics { contentDescription = "StreetView" },
                cameraPositionState = cameraPositionState,
                streetViewPanoramaOptionsFactory = {
                    StreetViewPanoramaOptions()
                        .position(initialLatLng)
                },
                onClick = onClick
            )
        }
        composeTestRule.waitUntil(8000) {
            cameraPositionState.location.position.latitude != 0.0 &&
                cameraPositionState.location.position.longitude != 0.0
        }
    }

    @Test
    fun testStartingStreetViewPosition() {
        initStreetView()
        initialLatLng.assertEquals(cameraPositionState.location.position)
    }
}