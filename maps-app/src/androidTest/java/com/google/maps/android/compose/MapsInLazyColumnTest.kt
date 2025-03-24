package com.google.maps.android.compose

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
        MapListItem(id = "1", location = LatLng(1.23, 4.56), zoom = 10f, title = "Item 1"),
        MapListItem(id = "2", location = LatLng(7.89, 0.12), zoom = 12f, title = "Item 2"),
        MapListItem(id = "3", location = LatLng(3.45, 6.78), zoom = 11f, title = "Item 3"),
        MapListItem(id = "4", location = LatLng(9.01, 2.34), zoom = 13f, title = "Item 4"),
        MapListItem(id = "5", location = LatLng(5.67, 8.90), zoom = 9f, title = "Item 5"),
        MapListItem(id = "6", location = LatLng(4.32, 7.65), zoom = 14f, title = "Item 6"),
        MapListItem(id = "7", location = LatLng(8.76, 1.23), zoom = 10f, title = "Item 7"),
        MapListItem(id = "8", location = LatLng(2.98, 6.54), zoom = 12f, title = "Item 8"),
        MapListItem(id = "9", location = LatLng(7.65, 3.21), zoom = 11f, title = "Item 9"),
        MapListItem(id = "10", location = LatLng(0.12, 9.87), zoom = 13f, title = "Item 10"),
    )


    private lateinit var cameraPositionStates: Map<MapItemId, CameraPositionState>

    private fun initMaps() {
        check(hasValidApiKey) { "Maps API key not specified" }

        composeTestRule.setContent {
            val lazyListState = rememberLazyListState()
            val visibleMapCount = remember { mutableStateOf(0) }

            val visibleItems by remember {
                derivedStateOf {
                    lazyListState.layoutInfo.visibleItemsInfo.size
                }
            }

            LaunchedEffect(visibleItems) {
                visibleMapCount.value = visibleItems
            }

            val countDownLatch = CountDownLatch(visibleMapCount.value)

            MapsInLazyColumn(
                mapItems,
                lazyListState = lazyListState,
                onMapLoaded = {
                    countDownLatch.countDown()
                }
            )

            LaunchedEffect(Unit) {
                val mapsLoaded = countDownLatch.await(30, TimeUnit.SECONDS)
                assertTrue("Visible maps loaded", mapsLoaded)
            }
        }
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

    @Test
    fun testScrollToBottom() {
        initMaps()
        composeTestRule.onRoot().performTouchInput { swipeUp(durationMillis = 1000) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("Item 5").assertIsDisplayed()
    }
}
