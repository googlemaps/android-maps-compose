package com.google.maps.android.compose.navigation.repositories

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.location.Location
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await

class LocationProvider(private val applicationContext: Context) {

    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    suspend fun getLastLocation(): Location? {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        return fusedLocationClient.lastLocation.await()
    }
}
