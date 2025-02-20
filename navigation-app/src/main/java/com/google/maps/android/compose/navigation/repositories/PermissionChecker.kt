package com.google.maps.android.compose.navigation.repositories

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class PermissionChecker(private val applicationContext: Context) {
    fun isGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(applicationContext, permission) == PackageManager.PERMISSION_GRANTED
    }
}
