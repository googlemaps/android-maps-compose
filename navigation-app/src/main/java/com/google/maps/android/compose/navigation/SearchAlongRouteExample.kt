package com.google.maps.android.compose.navigation

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.navigation.NavigationApi.NavigatorListener
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.places.api.model.EncodedPolyline
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.SearchAlongRouteParameters
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.maps.android.ktx.utils.latLngListEncode
import com.google.android.libraries.places.api.net.kotlin.awaitSearchByText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Holds an instance of the navigator class.  Most likely an activity or a view model.  Take a look at NavigationViewModel
 */
class NavigationContainer(
    private val placesClient: PlacesClient,
    private val scope: CoroutineScope,
) : NavigatorListener {
    private var navigator: Navigator? = null

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
        // Could show a snackbar or toast here
        Log.w("NavigationContainer", message)
    }

    override fun onNavigatorReady(navigator: Navigator?) {
        this.navigator = navigator ?: error("Navigator is null")
        navigator.addRouteChangedListener {
            Log.d("NavigationContainer", "Route changed")
            scope.launch {
                navigator.currentRouteSegment?.latLngs?.let { route ->
                    //pass the encoded string to a function to perform the search
                    searchPlacesAlongRoute(route)
                }
            }
        }
    }

    private suspend fun searchPlacesAlongRoute(route: List<LatLng>) {
        val encodedPolyline = EncodedPolyline.newInstance(route.latLngListEncode())
        val placeFields = listOf(Place.Field.ID, Place.Field.DISPLAY_NAME)

        val searchAlongRouteParameters = SearchAlongRouteParameters.newInstance(encodedPolyline)

        val response = placesClient.awaitSearchByText("Spicy Vegetarian Food", placeFields) {
            setSearchAlongRouteParameters(searchAlongRouteParameters)
            maxResultCount = 10
        }

        response.places.forEach {
            Log.d("Places API", "Place ID: ${it.id}, Display Name: ${it.displayName}")
        }
    }
}
