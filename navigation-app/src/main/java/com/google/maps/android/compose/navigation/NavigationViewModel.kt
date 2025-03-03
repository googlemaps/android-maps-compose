package com.google.maps.android.compose.navigation


import android.Manifest
import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.navigation.NavigationApi.NavigatorListener
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.RoutingOptions
import com.google.android.libraries.navigation.SimulationOptions
import com.google.android.libraries.navigation.Waypoint
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.maps.android.compose.navigation.repositories.LocationProvider
import com.google.maps.android.compose.navigation.repositories.PermissionChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.libraries.navigation.ListenableResultFuture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import com.google.maps.android.compose.navigation.BuildConfig

class NavigationViewModel(
    private val placesClient: PlacesClient,
    private val locationProvider: LocationProvider,
    private val permissionChecker: PermissionChecker,
) : ViewModel(), NavigatorListener {

    private fun String.isPermissionGranted() = permissionChecker.isGranted(this)

    private val _location = MutableStateFlow<LatLng?>(null)
    val location = _location.asStateFlow().onStart {
        requestLocation()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    private val _hasLocationPermission = MutableStateFlow(false)

    private var navigator: Navigator? = null

    init {
        viewModelScope.launch {
            _hasLocationPermission.collect() {
                if (it) {
                    requestLocation()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun requestLocation() {
        viewModelScope.launch {
            if (Manifest.permission.ACCESS_FINE_LOCATION.isPermissionGranted() || Manifest.permission.ACCESS_COARSE_LOCATION.isPermissionGranted()) {
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
            _hasLocationPermission.value = Manifest.permission.ACCESS_FINE_LOCATION.isPermissionGranted() || Manifest.permission.ACCESS_COARSE_LOCATION.isPermissionGranted()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val application =
                    checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as NavigationApplication

                return NavigationViewModel(
                    placesClient = application.placesClient,
                    locationProvider = LocationProvider(application.applicationContext),
                    permissionChecker = PermissionChecker(application.applicationContext)
                ) as T
            }
        }
    }

    private fun navigateToPlace(placeId: String, routingOptions: RoutingOptions) {
        val localNavigator = checkNotNull(navigator)

        viewModelScope.launch {
            try {
                val destination = withContext(Dispatchers.IO) {
                    Waypoint.builder().setPlaceIdString(placeId).build()
                }

                val cancellationTokenSource = CancellationTokenSource()
                val routeStatusFuture = localNavigator.setDestination(destination, routingOptions)

                suspendCoroutine { continuation ->
                    val callback =
                        ListenableResultFuture.OnResultListener<Navigator.RouteStatus> { status ->
                            fun onFailure(message: String) {
                                continuation.resumeWithException(Exception(message))
                            }

                            when (status) {
                                Navigator.RouteStatus.OK -> {
                                    //    // Hide the toolbar to maximize the navigation UI.
                                    //    if (getActionBar() != null) {
                                    //        getActionBar().hide()
                                    //    }
                                    //    // Enable voice audio guidance (through the device speaker).
                                    //    navigator.setAudioGuidance(
                                    //        Navigator.AudioGuidance.VOICE_ALERTS_AND_GUIDANCE
                                    //    )
                                    // Simulate vehicle progress along the route for demo/debug builds.
                                    if (BuildConfig.DEBUG) {
                                        localNavigator.simulator.simulateLocationsAlongExistingRoute(
                                            SimulationOptions().speedMultiplier(5f)
                                        )
                                    }

                                    // Start turn-by-turn guidance along the current route.
                                    localNavigator.startGuidance()

                                    continuation.resume(Unit)
                                }

                                Navigator.RouteStatus.NO_ROUTE_FOUND -> onFailure("Error starting navigation: No route found")
                                Navigator.RouteStatus.NETWORK_ERROR -> onFailure("Error starting navigation: Network error")
                                Navigator.RouteStatus.QUOTA_CHECK_FAILED -> onFailure("Error starting navigation: Quota check failed")
                                Navigator.RouteStatus.ROUTE_CANCELED -> onFailure("Error starting navigation: Route canceled")
                                Navigator.RouteStatus.LOCATION_DISABLED -> onFailure("Error starting navigation: Location disabled")
                                Navigator.RouteStatus.LOCATION_UNKNOWN -> onFailure("Error starting navigation: Location unknown")
                                Navigator.RouteStatus.WAYPOINT_ERROR -> onFailure("Error starting navigation: Waypoint error")

                                else -> onFailure("Error starting navigation: $status")
                            }
                        }

                    routeStatusFuture.setOnResultListener(callback)
                }
            } catch (e: Waypoint.UnsupportedPlaceIdException) {
                withContext(Dispatchers.Main) {
                    displayMessage("Error starting navigation: Place ID is not supported.")
                }
            }
        }
    }

    override fun onNavigatorReady(navigator: Navigator?) {
        displayMessage("navigator ready")

        val chautauquaDinningHall = "ChIJ9zb1-0bsa4cRcpW_h34lLBU"

        this.navigator = navigator ?: error("Navigator is null")

        /*
         // Optional. Disable the guidance notifications and shut down the app
                // and background service when the user closes the app.
                // mNavigator.setTaskRemovedBehavior(Navigator.TaskRemovedBehavior.QUIT_SERVICE)

                // Optional. Set the last digit of the car's license plate to get
                // route restrictions for supported countries.
                // mNavigator.setLicensePlateRestrictionInfo(getLastDigit(), "BZ");

                // Set the camera to follow the device location with 'TILTED' driving view.
                mNavFragment.getCamera().followMyLocation(Camera.Perspective.TILTED);

                // Set the travel mode (DRIVING, WALKING, CYCLING, TWO_WHEELER, or TAXI).
                mRoutingOptions = new RoutingOptions();
                mRoutingOptions.travelMode(RoutingOptions.TravelMode.DRIVING);

                // Navigate to a place, specified by Place ID.
                navigateToPlace(SYDNEY_OPERA_HOUSE, mRoutingOptions);
         */

        val routingOptions = RoutingOptions().apply {
            travelMode(RoutingOptions.TravelMode.DRIVING)
        }

        navigateToPlace(chautauquaDinningHall, routingOptions)
    }

    override fun onError(@NavigationApi.ErrorCode errorCode: Int) {
        when (errorCode) {
            NavigationApi.ErrorCode.NOT_AUTHORIZED -> displayMessage(
                "Error loading Navigation SDK: Your API key is "
                        + "invalid or not authorized to use the Navigation SDK."
            )

            NavigationApi.ErrorCode.TERMS_NOT_ACCEPTED -> displayMessage(
                "Error loading Navigation SDK: User did not accept "
                        + "the Navigation Terms of Use."
            )

            NavigationApi.ErrorCode.NETWORK_ERROR -> displayMessage("Error loading Navigation SDK: Network error.")
            NavigationApi.ErrorCode.LOCATION_PERMISSION_MISSING -> displayMessage(
                "Error loading Navigation SDK: Location permission "
                        + "is missing."
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
}

