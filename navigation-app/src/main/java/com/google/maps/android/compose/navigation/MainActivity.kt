package com.google.maps.android.compose.navigation

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.navigation.NavigationApi
import com.google.maps.android.compose.navigation.ui.theme.AndroidmapscomposeTheme
import kotlinx.coroutines.launch

val defaultLocation = LatLng(39.9828503662161, -105.71835147137016)

@OptIn(ExperimentalPermissionsApi::class)
class MainActivity : ComponentActivity() {
    private val myViewModel: NavigationViewModel by viewModels { NavigationViewModel.Factory }

    override fun onResume() {
        super.onResume()
        myViewModel.checkLocationPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NavigationApi.getNavigator(this, myViewModel)

        setContent {
            val context = LocalContext.current
            val snackbarHostState by remember { mutableStateOf(SnackbarHostState()) }
            val scope = rememberCoroutineScope()
            val locationPermissionsState = rememberMultiplePermissionsState(
                listOf(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION,)
            )

            val location by myViewModel.location.collectAsStateWithLifecycle()

            fun onShowSnackbar(message: String) {
                scope.launch {
                    val duration = if (message.length > 40)
                        SnackbarDuration.Long else SnackbarDuration.Short
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = duration
                    )
                }
            }

            LaunchedEffect(myViewModel, context) {
                myViewModel.uiEvent.collect { event ->
                    when (event) {
                        is UiEvent.ShowSnackbar -> onShowSnackbar(event.message)
                        is UiEvent.RequestLocationPermission -> {
                            locationPermissionsState.launchMultiplePermissionRequest()
                        }
                    }
                }
            }

            AndroidmapscomposeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavigationScreen(
                        modifier = Modifier.padding(innerPadding),
                        deviceLocation = location
                    )
                }
            }
        }
    }
}

