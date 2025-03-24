package com.google.maps.android.compose

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MapsInLazyColumnTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mapItems = listOf(
        MapListItem(id = "1", location = LatLng(1.23, 4.56), zoom = 10f, title = "A"),
        MapListItem(id = "2", location = LatLng(7.89, 0.12), zoom = 12f, title = "B"),
    )

    private lateinit var cameraPositionStates: Map<MapItemId, CameraPositionState>

    private fun initMaps() {
        check(hasValidApiKey) { "Maps API key not specified" }
        val countDownLatch = CountDownLatch(mapItems.size)
        composeTestRule.setContent {
            MapsInLazyColumn(mapItems, onMapLoaded = {
                countDownLatch.countDown()
            })
        }

        val mapsLoaded = countDownLatch.await(30, TimeUnit.SECONDS)
        assertTrue("Maps loaded", mapsLoaded)
    }

    @Before
    fun setUp() {
        cameraPositionStates = mapItems.associate { item ->
            item.id to CameraPositionState(
                position = CameraPosition.fromLatLngZoom(item.location, item.zoom)
            )
        }
    }

    @Test
    fun testStartingCameraPositions() {
        initMaps()
        mapItems.forEach { item ->
            item.location.assertEquals(cameraPositionStates[item.id]?.position?.target!!)
        }
    }

    @Test
    fun testLazyColumnScrolls_MapPositionsRemain() {
        initMaps()
        composeTestRule.onRoot().performTouchInput { swipeUp() }
        composeTestRule.waitForIdle()

        mapItems.forEach { item ->
            item.location.assertEquals(cameraPositionStates[item.id]?.position?.target!!)
        }
    }
}
