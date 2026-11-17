/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.maps.android.compose.navigation

import android.app.Application
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.maps.android.compose.navigation.repositories.ApiKeyProvider

class NavigationApplication : Application() {
    private val defaultPlacesClient: PlacesClient by lazy {
        Places.createClient(this)
    }

    var customPlacesClient: PlacesClient? = null

    val placesClient: PlacesClient
        get() = customPlacesClient ?: defaultPlacesClient

    override fun onCreate() {
        super.onCreate()
        val keyProvider = ApiKeyProvider(this)
        Places.initializeWithNewPlacesApiEnabled(applicationContext, keyProvider.placesApiKey)
    }
}
