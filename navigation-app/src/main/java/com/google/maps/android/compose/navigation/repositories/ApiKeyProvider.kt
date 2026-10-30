package com.google.maps.android.compose.navigation.repositories

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
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

    val placesApiKey: String by lazy {
        BuildConfig.PLACES_API_KEY.takeIf {
            it.isNotBlank() && it != "DEFAULT_API_KEY" && it != "YOUR_API_KEY"
        } ?: mapsApiKey
    }

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
            error("MapsApiKey missing from AndroidManifest.")
        }
        return mapsApiKey
    }

    init {
        if (placesApiKey == "DEFAULT_API_KEY" ||
            placesApiKey == "YOUR_API_KEY" ||
            mapsApiKey == "DEFAULT_API_KEY" ||
            mapsApiKey == "YOUR_API_KEY"
        ) {
            val message = "One or more API keys have not been set. Please see the README.md file."
            Log.w("ApiKeyProvider", message)
            Toast.makeText(
                context,
                message,
                Toast.LENGTH_LONG,
            ).show()
        }
    }
}
