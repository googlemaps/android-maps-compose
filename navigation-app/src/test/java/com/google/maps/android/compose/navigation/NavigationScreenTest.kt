package com.google.maps.android.compose.navigation

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.google.android.maps.robolectric.annotation.EnableMapsShadows
import com.google.android.maps.robolectric.shadows.ShadowGoogleMap
import com.google.android.maps.robolectric.shadows.ShadowMarker
import com.google.android.maps.robolectric.truth.LatLngSubject
import com.google.common.truth.Truth.assertThat
import com.google.maps.android.compose.navigation.shadows.ShadowNavBitmapDescriptorFactory
import com.google.maps.android.compose.navigation.shadows.ShadowNavCameraUpdateFactory
import com.google.maps.android.compose.navigation.shadows.ShadowNavGoogleMap
import com.google.maps.android.compose.navigation.shadows.ShadowNavigationView
import com.google.maps.android.compose.navigation.shadows.findNavigationView
import com.google.maps.android.compose.navigation.ui.theme.AndroidmapscomposeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    application = Application::class,
    shadows = [
        ShadowNavigationView::class,
        ShadowNavGoogleMap::class,
        ShadowNavCameraUpdateFactory::class,
        ShadowNavBitmapDescriptorFactory::class,
    ],
)
@EnableMapsShadows
class NavigationScreenTest {

    @get:Rule
    val composeTestRule: AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity> =
        createAndroidComposeRule<ComponentActivity>()

    private val bigfootCoordinates = LatLng(39.99932703674056, -105.28152457787887)
    private val boulderCoordinates = LatLng(40.014984, -105.270546)

    @Test
    fun navigationScreen_mountsNavigationView_andRendersBigfootComposableMarker() {
        composeTestRule.setContent {
            AndroidmapscomposeTheme {
                NavigationScreen(
                    deviceLocation = null,
                    placesAlongRoute = emptyList(),
                    onClearSearchResults = {},
                    onSearchClicked = {},
                    routeReady = false,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        composeTestRule.waitForIdle()

        val rootView = composeTestRule.activity.window.decorView
        val navigationView = rootView.findNavigationView()
        assertThat(navigationView).isNotNull()

        val shadowNavView = Shadow.extract(navigationView) as ShadowNavigationView
        assertThat(shadowNavView.isOnCreateCalled).isTrue()
        assertThat(shadowNavView.isOnStartCalled).isTrue()
        assertThat(shadowNavView.isOnResumeCalled).isTrue()

        val googleMap = shadowNavView.googleMap
        assertThat(googleMap).isNotNull()
        val shadowMap = Shadow.extract(googleMap) as ShadowGoogleMap

        assertThat(shadowMap.markers).hasSize(1)
        val bigfootMarker = shadowMap.markers.first()
        assertThat(bigfootMarker.title).isEqualTo("Bigfoot")
        LatLngSubject.assertThat(bigfootMarker.position)
            .isWithin(1.0)
            .of(bigfootCoordinates)

        val shadowMarker = Shadow.extract(bigfootMarker) as ShadowMarker
        assertThat(shadowMarker.iconBitmap).isNotNull()
        assertThat(shadowMarker.iconBitmap.width).isGreaterThan(0)
        assertThat(shadowMarker.iconBitmap.height).isGreaterThan(0)
    }

    @Test
    fun navigationScreen_whenDeviceLocationProvidedAndUpdated_movesUserMarkerAndCamera() {
        var currentDeviceLocation by mutableStateOf<LatLng?>(null)

        composeTestRule.setContent {
            AndroidmapscomposeTheme {
                NavigationScreen(
                    deviceLocation = currentDeviceLocation,
                    placesAlongRoute = emptyList(),
                    onClearSearchResults = {},
                    onSearchClicked = {},
                    routeReady = false,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        composeTestRule.waitForIdle()

        val shadowNavView = Shadow.extract(ShadowNavigationView.instances.single()) as ShadowNavigationView
        val googleMap = checkNotNull(shadowNavView.googleMap)
        val shadowMap = Shadow.extract(googleMap) as ShadowGoogleMap

        assertThat(shadowMap.markers.map { it.title }).containsExactly("Bigfoot")

        currentDeviceLocation = boulderCoordinates
        composeTestRule.waitForIdle()

        assertThat(shadowMap.markers.map { it.title }).containsExactly("User location", "Bigfoot")
        val userMarker = shadowMap.markers.first { it.title == "User location" }
        LatLngSubject.assertThat(userMarker.position)
            .isWithin(1.0)
            .of(boulderCoordinates)
        LatLngSubject.assertThat(googleMap.cameraPosition.target)
            .isWithin(1.0)
            .of(boulderCoordinates)
        assertThat(googleMap.cameraPosition.zoom).isEqualTo(15f)
    }

    @Test
    fun navigationScreen_expandablePlaceSearch_alongRouteToggle_andPlaceResultsWithMapMarkers() {
        var routeReady by mutableStateOf(false)
        var placesAlongRoute by mutableStateOf<List<Place>>(emptyList())
        val alongRouteQueries = mutableListOf<String>()
        val destinationQueries = mutableListOf<String>()
        val selectedPlaces = mutableListOf<Place>()
        var clearClickedCount = 0

        composeTestRule.setContent {
            AndroidmapscomposeTheme {
                NavigationScreen(
                    deviceLocation = boulderCoordinates,
                    placesAlongRoute = placesAlongRoute,
                    onClearSearchResults = {
                        clearClickedCount++
                        placesAlongRoute = emptyList()
                    },
                    onSearchClicked = { query ->
                        alongRouteQueries.add(query)
                    },
                    onDestinationSearchClicked = { query ->
                        destinationQueries.add(query)
                    },
                    onPlaceSelected = { place ->
                        selectedPlaces.add(place)
                    },
                    routeReady = routeReady,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        composeTestRule.waitForIdle()

        // Tap "Search" in the floating controls -> opens the search card to search for any destination
        composeTestRule.onNodeWithText("Search").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Search any destination...").performTextInput("Red Rocks")
        composeTestRule.onNodeWithText("Find").performClick()
        assertThat(destinationQueries).containsExactly("Red Rocks")

        // When routeReady becomes true -> "Along Route" chip appears in the search card
        routeReady = true
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Along Route").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Find").performClick()
        assertThat(alongRouteQueries).containsExactly("Red Rocks")

        // Populate placesAlongRoute with locations -> renders list AND map markers
        val leafCoords = LatLng(40.0165, -105.2760)
        val place1 = Place.builder()
            .setId("place-1")
            .setDisplayName("Leaf Vegetarian Restaurant")
            .setLocation(leafCoords)
            .build()
        placesAlongRoute = listOf(place1)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Leaf Vegetarian Restaurant").assertIsDisplayed()
        composeTestRule.onNodeWithText("Navigate").assertIsDisplayed().performClick()
        assertThat(selectedPlaces).containsExactly(place1)

        // Verify marker for Leaf Vegetarian Restaurant is plotted on GoogleMap
        val shadowNavView = Shadow.extract(ShadowNavigationView.instances.single()) as ShadowNavigationView
        val shadowMap = Shadow.extract(checkNotNull(shadowNavView.googleMap)) as ShadowGoogleMap
        assertThat(shadowMap.markers.map { it.title }).contains("Leaf Vegetarian Restaurant")

        // Clicking Close clears the search results and collapses the search overlay
        composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        assertThat(clearClickedCount).isEqualTo(1)
    }

    @Test
    fun navigationScreen_collapsibleControls_autoCollapseOnIdle_andExpandOnClick() {
        var simulationState by mutableStateOf(SimulationState.RUNNING)
        var simulationSpeed by mutableFloatStateOf(5f)
        var stopCount = 0

        composeTestRule.setContent {
            AndroidmapscomposeTheme {
                NavigationScreen(
                    deviceLocation = boulderCoordinates,
                    placesAlongRoute = emptyList(),
                    onClearSearchResults = {},
                    onSearchClicked = {},
                    routeReady = true,
                    simulationState = simulationState,
                    simulationSpeed = simulationSpeed,
                    onSimulationSpeedChanged = { simulationSpeed = it },
                    onPauseSimulation = { simulationState = SimulationState.PAUSED },
                    onResumeSimulation = { simulationState = SimulationState.RUNNING },
                    onStopNavigation = {
                        stopCount++
                        simulationState = SimulationState.STOPPED
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        composeTestRule.waitForIdle()

        // Initially expanded -> "Search", "5x", "Pause", "Stop" are visible
        composeTestRule.onNodeWithText("5x").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("10x").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertThat(simulationSpeed).isEqualTo(10f)

        // Verify Pause -> Resume flow
        composeTestRule.onNodeWithText("Pause").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertThat(simulationState).isEqualTo(SimulationState.PAUSED)

        composeTestRule.onNodeWithText("Resume").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertThat(simulationState).isEqualTo(SimulationState.RUNNING)

        // Verify Stop button stops navigation
        composeTestRule.onNodeWithText("Stop").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertThat(stopCount).isEqualTo(1)
        assertThat(simulationState).isEqualTo(SimulationState.STOPPED)

        // Advance clock past CONTROLS_AUTO_COLLAPSE_DELAY_MS -> controls collapse into single FAB
        composeTestRule.mainClock.advanceTimeBy(CONTROLS_AUTO_COLLAPSE_DELAY_MS + 500L)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Expand controls").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Search").assertIsDisplayed()
    }
}
