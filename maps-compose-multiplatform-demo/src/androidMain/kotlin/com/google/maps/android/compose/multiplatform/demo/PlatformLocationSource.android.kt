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

package com.google.maps.android.compose.multiplatform.demo

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.maps.android.location.LocationPriority
import com.google.maps.android.location.LocationSource
import com.google.maps.android.location.asLocationSource
import com.google.maps.android.location.coarseLocationEvents
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.merge

private const val LOCATION_PERMISSION_REQUEST_CODE = 1001

private fun Context.hasAnyLocationPermission(): Boolean =
    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun Context.hasFineLocationPermission(): Boolean =
    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@SuppressLint("MissingPermission")
@Composable
internal actual fun rememberPlatformLocationController(): PlatformLocationController {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val activity = remember(context) { context.findActivity() }
    var hasPermission by remember { mutableStateOf(appContext.hasAnyLocationPermission()) }

    // Continuously observe runtime permission changes so granting permission via the system
    // dialog or revoking/re-granting it in Android Settings updates the UI immediately.
    LaunchedEffect(appContext) {
        while (true) {
            hasPermission = appContext.hasAnyLocationPermission()
            delay(500L)
        }
    }

    val locationSource = remember(appContext) {
        val manager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        LocationSource { intervalMs, minUpdateDistanceM, priority ->
            channelFlow {
                if (!appContext.hasAnyLocationPermission()) {
                    return@channelFlow
                }

                // Safely query cached locations across providers; wrap each call in runCatching
                // so that coarse-only ("Approximate") permission or a mid-flight revocation
                // never throws an unhandled SecurityException.
                val lastKnown = runCatching {
                    manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                }.getOrNull()
                    ?: runCatching {
                        manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    }.getOrNull()
                    ?: runCatching {
                        manager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                    }.getOrNull()

                if (lastKnown != null) {
                    send(lastKnown)
                }

                val stream = if (appContext.hasFineLocationPermission() &&
                    priority == LocationPriority.HIGH_ACCURACY
                ) {
                    merge(
                        manager.asLocationSource().locationEvents(
                            intervalMs = intervalMs,
                            minUpdateDistanceM = minUpdateDistanceM,
                            priority = LocationPriority.HIGH_ACCURACY,
                        ),
                        manager.coarseLocationEvents(
                            minTimeMs = intervalMs,
                            minDistanceM = minUpdateDistanceM,
                        ),
                    )
                } else {
                    manager.coarseLocationEvents(
                        minTimeMs = intervalMs,
                        minDistanceM = minUpdateDistanceM,
                    )
                }

                stream.collect { send(it) }
            }
        }
    }

    val openAppSettings: () -> Unit = remember(appContext) {
        {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", appContext.packageName, null),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
        }
    }

    val requestPermission: () -> Unit = remember(activity, appContext, openAppSettings) {
        {
            if (activity == null) {
                openAppSettings()
            } else {
                activity.requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                    LOCATION_PERMISSION_REQUEST_CODE,
                )
                // On Android 11+, if the permission has the USER_FIXED flag set (because the user
                // previously denied it twice or revoked it in Settings), Activity.requestPermissions
                // silently drops the request ("No requestable permission in the request") without
                // launching GrantPermissionsActivity. If our Activity still holds window focus
                // 300ms later and permission is still not granted, fall back to opening App Settings.
                Handler(Looper.getMainLooper()).postDelayed({
                    if (!appContext.hasAnyLocationPermission() && activity.hasWindowFocus()) {
                        openAppSettings()
                    }
                }, 300L)
            }
        }
    }

    return remember(locationSource, hasPermission, requestPermission, openAppSettings) {
        PlatformLocationController(
            locationSource = locationSource,
            hasPermission = hasPermission,
            requestPermission = requestPermission,
            openAppSettings = openAppSettings,
        )
    }
}
