package com.google.maps.android.compose.navigation

import android.Manifest
import android.app.Application
import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.navigation.ListenableResultFuture
import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.RouteSegment
import com.google.android.libraries.navigation.RoutingOptions
import com.google.android.libraries.navigation.Simulator
import com.google.android.libraries.navigation.Waypoint
import com.google.android.libraries.places.api.model.EncodedPolyline
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchByTextRequest
import com.google.android.libraries.places.api.net.SearchByTextResponse
import com.google.common.truth.Truth.assertThat
import com.google.maps.android.compose.navigation.repositories.LocationProvider
import com.google.maps.android.compose.navigation.repositories.PermissionChecker
import com.google.maps.android.latLngListEncode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NavigationViewModelTest {

    private lateinit var placesClient: PlacesClient
    private lateinit var locationProvider: LocationProvider
    private lateinit var permissionChecker: PermissionChecker

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        placesClient = mockk(relaxed = true)
        locationProvider = mockk(relaxed = true)
        permissionChecker = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        ShadowLooper.idleMainLooper()
        Dispatchers.resetMain()
    }

    @Test
    fun requestLocation_whenPermissionMissing_emitsRequestLocationPermissionEvent() = runTest {
        every { permissionChecker.isGranted(any()) } returns false
        val viewModel = NavigationViewModel(placesClient, locationProvider, permissionChecker)

        val events = mutableListOf<UiEvent>()
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvent.collect { events.add(it) }
        }

        viewModel.requestLocation()
        advanceUntilIdle()

        assertThat(events).contains(UiEvent.RequestLocationPermission)
        collectJob.cancel()
    }

    @Test
    fun checkLocationPermission_whenGranted_fetchesLocationAndUpdatesStateFlow() = runTest {
        every { permissionChecker.isGranted(Manifest.permission.ACCESS_FINE_LOCATION) } returns true
        val androidLocation = Location("fused").apply {
            latitude = 40.0150
            longitude = -105.2705
        }
        coEvery { locationProvider.getLastLocation() } returns androidLocation

        val viewModel = NavigationViewModel(placesClient, locationProvider, permissionChecker)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.location.collect {}
        }

        viewModel.checkLocationPermission()
        advanceUntilIdle()

        val current = viewModel.location.first { it != null }
        assertThat(current).isEqualTo(LatLng(40.0150, -105.2705))
        collectJob.cancel()
    }

    @Test
    fun onNavigatorReady_clearsStaleSimulatedLocation_andNavigateToPlaceStartsGuidance() = runTest {
        val navigator = mockk<Navigator>(relaxed = true)
        val simulator = mockk<Simulator>(relaxed = true)
        every { navigator.simulator } returns simulator

        val routeChangedListenerSlot = slot<Navigator.RouteChangedListener>()
        every { navigator.addRouteChangedListener(capture(routeChangedListenerSlot)) } answers {}

        val resultListenerSlot = slot<ListenableResultFuture.OnResultListener<Navigator.RouteStatus>>()
        val routeStatusFuture = mockk<ListenableResultFuture<Navigator.RouteStatus>>(relaxed = true)
        every { routeStatusFuture.setOnResultListener(capture(resultListenerSlot)) } answers {
            resultListenerSlot.captured.onResult(Navigator.RouteStatus.OK)
        }
        every { navigator.setDestination(any<Waypoint>(), any<RoutingOptions>()) } returns routeStatusFuture

        val viewModel = NavigationViewModel(placesClient, locationProvider, permissionChecker)
        val events = mutableListOf<UiEvent>()
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvent.collect { events.add(it) }
        }

        viewModel.onNavigatorReady(navigator)
        advanceUntilIdle()

        assertThat(events).contains(UiEvent.ShowSnackbar("navigator ready"))
        verify { navigator.setTaskRemovedBehavior(Navigator.TaskRemovedBehavior.QUIT_SERVICE) }
        verify { simulator.unsetUserLocation() }
        verify(exactly = 0) { navigator.setDestination(any<Waypoint>(), any<RoutingOptions>()) }
        assertThat(viewModel.simulationState.value).isEqualTo(SimulationState.IDLE)

        // Now navigate to a chosen place
        viewModel.navigateToPlace(DEFAULT_DESTINATION_PLACE_ID, label = "Chautauqua")
        for (i in 1..20) {
            if (resultListenerSlot.isCaptured) break
            Thread.sleep(20)
            ShadowLooper.idleMainLooper()
        }
        advanceUntilIdle()

        verify { navigator.setDestination(any<Waypoint>(), any<RoutingOptions>()) }
        verify { simulator.simulateLocationsAlongExistingRoute(any()) }
        verify { navigator.startGuidance() }
        assertThat(viewModel.simulationState.value).isEqualTo(SimulationState.RUNNING)

        assertThat(viewModel.routeReady.value).isFalse()
        routeChangedListenerSlot.captured.onRouteChanged()
        assertThat(viewModel.routeReady.value).isTrue()

        collectJob.cancel()
    }

    @Test
    fun simulationControls_pause_resume_changeSpeed_andStopNavigationClearsSimulatedLocation() = runTest {
        every { permissionChecker.isGranted(Manifest.permission.ACCESS_FINE_LOCATION) } returns true
        val realGpsLocation = Location("gps").apply {
            latitude = 37.4220
            longitude = -122.0841
        }
        coEvery { locationProvider.getLastLocation() } returns realGpsLocation

        val navigator = mockk<Navigator>(relaxed = true)
        val simulator = mockk<Simulator>(relaxed = true)
        every { navigator.simulator } returns simulator

        val resultListenerSlot = slot<ListenableResultFuture.OnResultListener<Navigator.RouteStatus>>()
        val routeStatusFuture = mockk<ListenableResultFuture<Navigator.RouteStatus>>(relaxed = true)
        every { routeStatusFuture.setOnResultListener(capture(resultListenerSlot)) } answers {
            resultListenerSlot.captured.onResult(Navigator.RouteStatus.OK)
        }
        every { navigator.setDestination(any<Waypoint>(), any<RoutingOptions>()) } returns routeStatusFuture

        val viewModel = NavigationViewModel(placesClient, locationProvider, permissionChecker)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.location.collect {}
        }

        viewModel.onNavigatorReady(navigator)
        viewModel.navigateToPlace(DEFAULT_DESTINATION_PLACE_ID, label = "Chautauqua")
        for (i in 1..20) {
            if (resultListenerSlot.isCaptured) break
            Thread.sleep(20)
            ShadowLooper.idleMainLooper()
        }
        advanceUntilIdle()
        assertThat(viewModel.simulationState.value).isEqualTo(SimulationState.RUNNING)

        // Change speed while running -> updates simulationSpeed and re-applies to simulator
        viewModel.setSimulationSpeed(10f)
        assertThat(viewModel.simulationSpeed.value).isEqualTo(10f)
        verify(exactly = 2) { simulator.simulateLocationsAlongExistingRoute(any()) }

        // Pause simulation
        viewModel.pauseSimulation()
        assertThat(viewModel.simulationState.value).isEqualTo(SimulationState.PAUSED)
        verify { simulator.pause() }

        // Resume simulation
        viewModel.resumeSimulation()
        assertThat(viewModel.simulationState.value).isEqualTo(SimulationState.RUNNING)
        verify { simulator.resume() }

        // Stop navigation -> stops guidance and clears destinations BEFORE unsetting simulated user location
        viewModel.stopNavigation()
        advanceUntilIdle()

        assertThat(viewModel.simulationState.value).isEqualTo(SimulationState.STOPPED)
        assertThat(viewModel.routeReady.value).isFalse()
        assertThat(viewModel.placesAlongRoute.value).isEmpty()
        verifyOrder {
            navigator.stopGuidance()
            navigator.clearDestinations()
            simulator.unsetUserLocation()
        }
        assertThat(viewModel.location.value).isEqualTo(LatLng(37.4220, -122.0841))
        collectJob.cancel()
    }

    @Test
    fun navigateToLatLng_updatesDestinationAndRoutes() = runTest {
        val navigator = mockk<Navigator>(relaxed = true)
        val simulator = mockk<Simulator>(relaxed = true)
        every { navigator.simulator } returns simulator

        val waypointSlot = slot<Waypoint>()
        val resultListenerSlot = slot<ListenableResultFuture.OnResultListener<Navigator.RouteStatus>>()
        val routeStatusFuture = mockk<ListenableResultFuture<Navigator.RouteStatus>>(relaxed = true)
        every { routeStatusFuture.setOnResultListener(capture(resultListenerSlot)) } answers {
            resultListenerSlot.captured.onResult(Navigator.RouteStatus.OK)
        }
        every { navigator.setDestination(capture(waypointSlot), any<RoutingOptions>()) } returns routeStatusFuture

        val viewModel = NavigationViewModel(placesClient, locationProvider, permissionChecker)
        viewModel.onNavigatorReady(navigator)

        val customPin = LatLng(40.0183, -105.2788)
        viewModel.navigateToLatLng(customPin, label = "Pearl Street Mall")
        for (i in 1..20) {
            if (waypointSlot.isCaptured && waypointSlot.captured.title == "Pearl Street Mall") break
            Thread.sleep(20)
            ShadowLooper.idleMainLooper()
        }
        advanceUntilIdle()

        assertThat(viewModel.destinationLabel.value).isEqualTo("Pearl Street Mall")
        assertThat(waypointSlot.captured.title).isEqualTo("Pearl Street Mall")
        assertThat(waypointSlot.captured.position).isEqualTo(customPin)
    }

    @Test
    fun navigateToPlace_whenRouteStatusFails_emitsSnackbarWithoutCrashing() = runTest {
        val navigator = mockk<Navigator>(relaxed = true)
        val resultListenerSlot = slot<ListenableResultFuture.OnResultListener<Navigator.RouteStatus>>()
        val routeStatusFuture = mockk<ListenableResultFuture<Navigator.RouteStatus>>(relaxed = true)
        every { routeStatusFuture.setOnResultListener(capture(resultListenerSlot)) } answers {
            resultListenerSlot.captured.onResult(Navigator.RouteStatus.NO_ROUTE_FOUND)
        }
        every { navigator.setDestination(any<Waypoint>(), any<RoutingOptions>()) } returns routeStatusFuture

        val viewModel = NavigationViewModel(placesClient, locationProvider, permissionChecker)
        val events = mutableListOf<UiEvent>()
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvent.collect { events.add(it) }
        }

        viewModel.onNavigatorReady(navigator)
        viewModel.navigateToPlace(DEFAULT_DESTINATION_PLACE_ID, label = "Chautauqua")
        for (i in 1..20) {
            if (resultListenerSlot.isCaptured) break
            Thread.sleep(20)
            ShadowLooper.idleMainLooper()
        }
        advanceUntilIdle()

        assertThat(events).contains(
            UiEvent.ShowSnackbar("Error starting navigation: No route found"),
        )
        assertThat(viewModel.simulationState.value).isEqualTo(SimulationState.STOPPED)
        collectJob.cancel()
    }

    @Test
    fun searchAlongRoute_encodesRoutePolyline_queriesPlacesClient_andClearsResults() = runTest {
        val navigator = mockk<Navigator>(relaxed = true)
        val routePoints = listOf(
            LatLng(39.98285, -105.71835),
            LatLng(39.99932, -105.28152),
        )
        val routeSegment = mockk<RouteSegment> {
            every { latLngs } returns routePoints
        }
        every { navigator.currentRouteSegment } returns routeSegment

        val routeStatusFuture = mockk<ListenableResultFuture<Navigator.RouteStatus>>(relaxed = true)
        every { navigator.setDestination(any<Waypoint>(), any<RoutingOptions>()) } returns routeStatusFuture

        val place1 = Place.builder()
            .setId("p1")
            .setDisplayName("Dushanbe Tea House")
            .setLocation(LatLng(40.0155, -105.2770))
            .build()
        val requestSlot = slot<SearchByTextRequest>()
        val searchResponseTask = Tasks.forResult(
            SearchByTextResponse.newInstance(listOf(place1)),
        )
        every { placesClient.searchByText(capture(requestSlot)) } returns searchResponseTask
        every { placesClient.zzf(capture(requestSlot), any()) } returns searchResponseTask

        val viewModel = NavigationViewModel(placesClient, locationProvider, permissionChecker)
        viewModel.onNavigatorReady(navigator)

        viewModel.searchAlongRoute("Spicy Vegetarian Food")
        for (i in 1..20) {
            ShadowLooper.idleMainLooper()
            if (viewModel.placesAlongRoute.value.isNotEmpty()) break
            Thread.sleep(20)
        }
        advanceUntilIdle()

        assertThat(viewModel.placesAlongRoute.value).containsExactly(place1)

        val capturedRequest = requestSlot.captured
        assertThat(capturedRequest.textQuery).isEqualTo("Spicy Vegetarian Food")
        assertThat(capturedRequest.maxResultCount).isEqualTo(10)
        val expectedPolyline = routePoints.latLngListEncode()
        val actualPolyline =
            (capturedRequest.searchAlongRouteParameters?.polyline as? EncodedPolyline)?.encodedPolyline
        assertThat(actualPolyline).isEqualTo(expectedPolyline)

        viewModel.clearSearchResults()
        assertThat(viewModel.placesAlongRoute.value).isEmpty()
    }

    @Test
    fun onError_emitsCorrespondingSnackbarMessage() = runTest {
        val viewModel = NavigationViewModel(placesClient, locationProvider, permissionChecker)
        val events = mutableListOf<UiEvent>()
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvent.collect { events.add(it) }
        }

        viewModel.onError(NavigationApi.ErrorCode.NOT_AUTHORIZED)
        viewModel.onError(NavigationApi.ErrorCode.TERMS_NOT_ACCEPTED)
        viewModel.onError(NavigationApi.ErrorCode.NETWORK_ERROR)
        viewModel.onError(NavigationApi.ErrorCode.LOCATION_PERMISSION_MISSING)
        advanceUntilIdle()

        val messages = events.filterIsInstance<UiEvent.ShowSnackbar>().map { it.message }
        assertThat(messages).containsExactly(
            "Error loading Navigation SDK: Your API key is invalid or not authorized to use the Navigation SDK.",
            "Error loading Navigation SDK: User did not accept the Navigation Terms of Use.",
            "Error loading Navigation SDK: Network error.",
            "Error loading Navigation SDK: Location permission is missing.",
        ).inOrder()
        collectJob.cancel()
    }
}
