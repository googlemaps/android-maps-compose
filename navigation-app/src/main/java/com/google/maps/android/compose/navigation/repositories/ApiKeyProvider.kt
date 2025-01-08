package com.google.maps.android.compose.navigation.repositories

import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import com.google.maps.android.compose.navigation.BuildConfig

/**
 * Provides the Google Maps API key from the AndroidManifest.xml file.
 *
 * @param context The application context.
 */
class ApiKeyProvider(private val context: Context) {
    val mapsApiKey: String by lazy {
        getMapsApiKeyFromManifest()
    }

    val placesApiKey: String = BuildConfig.PLACES_API_KEY

    private fun getMapsApiKeyFromManifest(): String {
        val mapsApiKey =
            try {
                val applicationInfo =
                    context.packageManager.getApplicationInfo(
                        context.packageName,
                        PackageManager.GET_META_DATA,
                    )
                applicationInfo.metaData?.getString("com.google.android.geo.API_KEY") ?: ""
            } catch (e: PackageManager.NameNotFoundException) {
                error("Unable to find package: ${e.message}")
            }
        if (mapsApiKey.isBlank()) {
            // TODO: get the right error message/behavior.
            error("MapsApiKey missing from AndroidManifest.")
        }
        return mapsApiKey
    }

    init {
        if (placesApiKey == "DEFAULT_API_KEY" || mapsApiKey == "DEFAULT_API_KEY") {
            Toast.makeText(
                context,
                "One or more API keys have not been set.  Please see the README.md file.",
                Toast.LENGTH_LONG
            ).show()
            error("One or more API keys have not been set.  Please see the README.md file.")
        }
    }
}