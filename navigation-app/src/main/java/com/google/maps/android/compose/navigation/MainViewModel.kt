package com.google.maps.android.compose.navigation


import android.Manifest
import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.maps.android.compose.navigation.repositories.LocationProvider
import com.google.maps.android.compose.navigation.repositories.PermissionChecker
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val placesClient: PlacesClient,
    private val locationProvider: LocationProvider,
    private val permissionChecker: PermissionChecker,
) : ViewModel() {

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

                return MainViewModel(
                    placesClient = application.placesClient,
                    locationProvider = LocationProvider(application.applicationContext),
                    permissionChecker = PermissionChecker(application.applicationContext)
                ) as T
            }
        }
    }
}