package com.google.maps.android.compose.navigation

import android.Manifest
import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.navigation.ListenableResultFuture
import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.navigation.NavigationApi.NavigatorListener
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.RoutingOptions
import com.google.android.libraries.navigation.SimulationOptions
import com.google.android.libraries.navigation.Waypoint
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.EncodedPolyline
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.SearchAlongRouteParameters
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.kotlin.awaitSearchByText
import com.google.maps.android.compose.navigation.repositories.LocationProvider
import com.google.maps.android.compose.navigation.repositories.PermissionChecker
import com.google.maps.android.latLngListEncode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

const val DEFAULT_SIMULATION_SPEED = 5f
const val DEFAULT_DESTINATION_PLACE_ID = "ChIJ9zb1-0bsa4cRcpW_h34lLBU"
const val DEFAULT_DESTINATION_NAME = ""
private const val DEFAULT_SEARCH_BIAS_RADIUS_METERS = 50_000.0

val SIMULATION_SPEED_OPTIONS = listOf(1f, 2f, 5f, 10f, 20f)

/**
 * Represents the lifecycle state of the turn-by-turn vehicle simulator.
 */
enum class SimulationState {
    IDLE,
    RUNNING,
    PAUSED,
    STOPPED,
}

class NavigationViewModel(
    private val placesClient: PlacesClient,
    private val locationProvider: LocationProvider,
    private val permissionChecker: PermissionChecker,
) : ViewModel(),
    NavigatorListener {

    private fun String.isPermissionGranted() = permissionChecker.isGranted(this)

    private val _location = MutableStateFlow<LatLng?>(null)
    val location = _location.asStateFlow().onStart {
        requestLocation()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null,
    )

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    private val hasLocationPermission = MutableStateFlow(false)

    private var navigator: Navigator? = null
    private var routeChangedListener: Navigator.RouteChangedListener? = null

    private val _placesAlongRoute = MutableStateFlow<List<Place>>(emptyList())
    val placesAlongRoute = _placesAlongRoute.asStateFlow()

    private val _routeReady = MutableStateFlow(false)
    val routeReady = _routeReady.asStateFlow()

    private val _simulationState = MutableStateFlow(SimulationState.IDLE)
    val simulationState: StateFlow<SimulationState> = _simulationState.asStateFlow()

    private val _simulationSpeed = MutableStateFlow(DEFAULT_SIMULATION_SPEED)
    val simulationSpeed: StateFlow<Float> = _simulationSpeed.asStateFlow()

    private val _destinationLabel = MutableStateFlow(DEFAULT_DESTINATION_NAME)
    val destinationLabel: StateFlow<String> = _destinationLabel.asStateFlow()

    init {
        viewModelScope.launch {
            hasLocationPermission.collect {
                if (it) {
                    requestLocation()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun requestLocation() {
        viewModelScope.launch {
            if (Manifest.permission.ACCESS_FINE_LOCATION.isPermissionGranted() ||
                Manifest.permission.ACCESS_COARSE_LOCATION.isPermissionGranted()
            ) {
                val location = locationProvider.getLastLocation()?.toLatLng()
                if (location != null) {
                    _location.value = location
                }
            } else {
                _uiEvent.emit(UiEvent.RequestLocationPermission)
            }
        }
    }

    fun checkLocationPermission() {
        viewModelScope.launch {
            hasLocationPermission.value =
                Manifest.permission.ACCESS_FINE_LOCATION.isPermissionGranted() ||
                Manifest.permission.ACCESS_COARSE_LOCATION.isPermissionGranted()
        }
    }

    /**
     * Updates the simulation speed multiplier (`1x`..`20x`).
     * If a route simulation is currently running, immediately applies the new speed to the simulator.
     */
    fun setSimulationSpeed(speedMultiplier: Float) {
        if (speedMultiplier <= 0f) return
        _simulationSpeed.value = speedMultiplier
        if (_simulationState.value == SimulationState.RUNNING) {
            navigator?.simulator?.simulateLocationsAlongExistingRoute(
                SimulationOptions().speedMultiplier(speedMultiplier),
            )
        }
    }

    /**
     * Pauses the active vehicle simulation along the route without clearing the route.
     */
    fun pauseSimulation() {
        val localNavigator = navigator ?: return
        localNavigator.simulator.pause()
        _simulationState.value = SimulationState.PAUSED
    }

    /**
     * Resumes a paused simulation, or restarts simulation along the active route if stopped.
     */
    fun resumeSimulation() {
        val localNavigator = navigator ?: return
        when (_simulationState.value) {
            SimulationState.PAUSED -> {
                localNavigator.simulator.resume()
                _simulationState.value = SimulationState.RUNNING
            }

            SimulationState.STOPPED, SimulationState.IDLE -> {
                if (_routeReady.value) {
                    localNavigator.simulator.simulateLocationsAlongExistingRoute(
                        SimulationOptions().speedMultiplier(_simulationSpeed.value),
                    )
                    localNavigator.startGuidance()
                    _simulationState.value = SimulationState.RUNNING
                }
            }

            SimulationState.RUNNING -> Unit
        }
    }

    /**
     * Stops turn-by-turn guidance and vehicle simulation, clears active destinations,
     * unsets the simulated user location override so the SDK reverts to real GPS,
     * and refreshes [_location] from the device's location provider.
     */
    fun stopNavigation() {
        val localNavigator = navigator
        if (localNavigator != null) {
            localNavigator.stopGuidance()
            localNavigator.clearDestinations()
            localNavigator.simulator.unsetUserLocation()
        }
        _simulationState.value = SimulationState.STOPPED
        _routeReady.value = false
        _destinationLabel.value = ""
        _placesAlongRoute.value = emptyList()
        _location.value = null
        requestLocation()
        displayMessage("Navigation stopped")
    }

    /**
     * Navigates to a [Place] selected from search results.
     */
    fun navigateToSelectedPlace(place: Place) {
        val label = place.displayName ?: "Selected Place"
        val placeId = place.id
        val location = place.location
        _placesAlongRoute.value = emptyList()
        if (placeId != null) {
            navigateToPlace(placeId = placeId, label = label)
        } else if (location != null) {
            navigateToLatLng(latLng = location, label = label)
        }
    }

    /**
     * Navigates to an arbitrary [LatLng] coordinate (e.g., chosen by long-pressing on the map).
     */
    fun navigateToLatLng(
        latLng: LatLng,
        label: String = String.format(Locale.US, "Pin (%.4f, %.4f)", latLng.latitude, latLng.longitude),
        routingOptions: RoutingOptions = defaultRoutingOptions(),
    ) {
        val localNavigator = navigator
        if (localNavigator == null) {
            displayMessage("Navigator is not ready yet.")
            return
        }
        _destinationLabel.value = label
        _routeReady.value = false

        viewModelScope.launch {
            val destination = withContext(Dispatchers.IO) {
                Waypoint.builder()
                    .setLatLng(latLng.latitude, latLng.longitude)
                    .setTitle(label)
                    .build()
            }
            startNavigationToWaypoint(localNavigator, destination, routingOptions)
        }
    }

    /**
     * Navigates to a Google Maps [placeId].
     */
    fun navigateToPlace(
        placeId: String,
        label: String = _destinationLabel.value,
        routingOptions: RoutingOptions = defaultRoutingOptions(),
    ) {
        val localNavigator = navigator
        if (localNavigator == null) {
            displayMessage("Navigator is not ready yet.")
            return
        }
        _destinationLabel.value = label
        _routeReady.value = false

        viewModelScope.launch {
            try {
                val destination = withContext(Dispatchers.IO) {
                    Waypoint.builder().setPlaceIdString(placeId).build()
                }
                startNavigationToWaypoint(localNavigator, destination, routingOptions)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Waypoint.UnsupportedPlaceIdException) {
                displayMessage("Error starting navigation: Place ID is not supported.")
            } catch (e: Exception) {
                displayMessage(e.message ?: "Error starting navigation.")
            }
        }
    }

    private suspend fun startNavigationToWaypoint(
        localNavigator: Navigator,
        destination: Waypoint,
        routingOptions: RoutingOptions,
    ) {
        try {
            val routeStatusFuture = localNavigator.setDestination(destination, routingOptions)

            suspendCoroutine { continuation ->
                val callback =
                    ListenableResultFuture.OnResultListener<Navigator.RouteStatus> { status ->
                        fun onFailure(message: String) {
                            continuation.resumeWithException(Exception(message))
                        }

                        when (status) {
                            Navigator.RouteStatus.OK -> {
                                // Simulate vehicle progress along the route for demo/debug builds.
                                if (BuildConfig.DEBUG) {
                                    localNavigator.simulator.simulateLocationsAlongExistingRoute(
                                        SimulationOptions().speedMultiplier(_simulationSpeed.value),
                                    )
                                }

                                // Start turn-by-turn guidance along the current route.
                                localNavigator.startGuidance()
                                _simulationState.value = SimulationState.RUNNING

                                continuation.resume(Unit)
                            }

                            Navigator.RouteStatus.NO_ROUTE_FOUND -> onFailure(
                                "Error starting navigation: No route found",
                            )

                            Navigator.RouteStatus.NETWORK_ERROR -> onFailure(
                                "Error starting navigation: Network error",
                            )

                            Navigator.RouteStatus.QUOTA_CHECK_FAILED -> onFailure(
                                "Error starting navigation: Quota check failed",
                            )

                            Navigator.RouteStatus.ROUTE_CANCELED -> onFailure(
                                "Error starting navigation: Route canceled",
                            )

                            Navigator.RouteStatus.LOCATION_DISABLED -> onFailure(
                                "Error starting navigation: Location disabled",
                            )

                            Navigator.RouteStatus.LOCATION_UNKNOWN -> onFailure(
                                "Error starting navigation: Location unknown",
                            )

                            Navigator.RouteStatus.WAYPOINT_ERROR -> onFailure(
                                "Error starting navigation: Waypoint error",
                            )

                            else -> onFailure("Error starting navigation: $status")
                        }
                    }

                routeStatusFuture.setOnResultListener(callback)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _simulationState.value = SimulationState.STOPPED
            displayMessage(e.message ?: "Error starting navigation.")
        }
    }

    override fun onNavigatorReady(navigator: Navigator?) {
        val readyNavigator = navigator ?: error("Navigator is null")
        this.navigator = readyNavigator
        readyNavigator.setTaskRemovedBehavior(Navigator.TaskRemovedBehavior.QUIT_SERVICE)
        // Clear any leftover simulated location from a prior run so we start at real GPS.
        readyNavigator.stopGuidance()
        readyNavigator.clearDestinations()
        readyNavigator.simulator.unsetUserLocation()
        displayMessage("navigator ready")

        // Listen for route changes
        routeChangedListener?.let { readyNavigator.removeRouteChangedListener(it) }
        val listener = Navigator.RouteChangedListener {
            Log.d("NavigationContainer", "Route changed")
            _routeReady.value = true
        }
        routeChangedListener = listener
        readyNavigator.addRouteChangedListener(listener)
    }

    override fun onCleared() {
        super.onCleared()
        routeChangedListener?.let { listener ->
            navigator?.removeRouteChangedListener(listener)
        }
        routeChangedListener = null
        navigator?.stopGuidance()
        navigator?.clearDestinations()
        navigator?.simulator?.unsetUserLocation()
    }

    /**
     * Searches for places matching [searchText].
     * When [alongRouteOnly] is true and an active route polyline exists, biases the search along
     * the current route via [SearchAlongRouteParameters]. Otherwise biases around the device's
     * location (or [defaultLocation]) so the user can search for any destination place.
     */
    private suspend fun searchPlaces(searchText: String, placeFields: List<Place.Field>, alongRouteOnly: Boolean) {
        if (searchText.isBlank()) return

        val route = if (alongRouteOnly) navigator?.currentRouteSegment?.latLngs else null
        val searchAlongRouteParameters = if (!route.isNullOrEmpty()) {
            val encodedPolyline = EncodedPolyline.newInstance(route.latLngListEncode())
            SearchAlongRouteParameters.newInstance(encodedPolyline)
        } else {
            null
        }

        val biasCenter = _location.value ?: defaultLocation

        try {
            val response = placesClient.awaitSearchByText(searchText, placeFields) {
                if (searchAlongRouteParameters != null) {
                    setSearchAlongRouteParameters(searchAlongRouteParameters)
                } else {
                    locationBias = CircularBounds.newInstance(
                        biasCenter,
                        DEFAULT_SEARCH_BIAS_RADIUS_METERS,
                    )
                }
                maxResultCount = 10
            }

            response.places.forEach {
                Log.d("NavigationViewModel", "Place ID: ${it.id}, Display Name: ${it.displayName}")
            }

            _placesAlongRoute.value = response.places
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            displayMessage("Error searching places: ${e.message}")
        }
    }

    override fun onError(@NavigationApi.ErrorCode errorCode: Int) {
        when (errorCode) {
            NavigationApi.ErrorCode.NOT_AUTHORIZED -> displayMessage(
                "Error loading Navigation SDK: Your API key is " +
                    "invalid or not authorized to use the Navigation SDK.",
            )

            NavigationApi.ErrorCode.TERMS_NOT_ACCEPTED -> displayMessage(
                "Error loading Navigation SDK: User did not accept " +
                    "the Navigation Terms of Use.",
            )

            NavigationApi.ErrorCode.NETWORK_ERROR -> displayMessage("Error loading Navigation SDK: Network error.")

            NavigationApi.ErrorCode.LOCATION_PERMISSION_MISSING -> displayMessage(
                "Error loading Navigation SDK: Location permission " +
                    "is missing.",
            )

            else -> displayMessage("Error loading Navigation SDK: $errorCode")
        }
    }

    private fun displayMessage(message: String) {
        Log.w("NavigationViewModel", message)
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.ShowSnackbar(message))
        }
    }

    fun clearSearchResults() {
        _placesAlongRoute.value = emptyList()
    }

    /**
     * Searches along the active route polyline when a route is available, or performs a
     * location-biased place search when no route is active.
     */
    fun searchAlongRoute(searchText: String) {
        viewModelScope.launch {
            searchPlaces(
                searchText = searchText,
                placeFields = DEFAULT_PLACE_FIELDS,
                alongRouteOnly = true,
            )
        }
    }

    /**
     * Searches for places (such as a new destination) biased around the user's current location,
     * regardless of whether a route is currently active.
     */
    fun searchDestinationPlaces(searchText: String) {
        viewModelScope.launch {
            searchPlaces(
                searchText = searchText,
                placeFields = DEFAULT_PLACE_FIELDS,
                alongRouteOnly = false,
            )
        }
    }

    private fun defaultRoutingOptions(): RoutingOptions = RoutingOptions().apply {
        travelMode(RoutingOptions.TravelMode.DRIVING)
    }

    companion object {
        private val DEFAULT_PLACE_FIELDS = listOf(
            Place.Field.ID,
            Place.Field.DISPLAY_NAME,
            Place.Field.LOCATION,
            Place.Field.FORMATTED_ADDRESS,
        )

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application =
                    checkNotNull(
                        extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY],
                    ) as NavigationApplication

                return NavigationViewModel(
                    placesClient = application.placesClient,
                    locationProvider = LocationProvider(application.applicationContext),
                    permissionChecker = PermissionChecker(application.applicationContext),
                ) as T
            }
        }
    }
}
