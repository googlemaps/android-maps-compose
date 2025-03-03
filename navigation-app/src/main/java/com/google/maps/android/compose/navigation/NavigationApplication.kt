package com.google.maps.android.compose.navigation

import android.app.Application
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.maps.android.compose.navigation.repositories.ApiKeyProvider

class NavigationApplication : Application() {
    val placesClient: PlacesClient by lazy {
        Places.createClient(this)
    }

    override fun onCreate() {
        super.onCreate()
        val keyProvider = ApiKeyProvider(this)
        Places.initializeWithNewPlacesApiEnabled(applicationContext, keyProvider.placesApiKey)
    }
}
