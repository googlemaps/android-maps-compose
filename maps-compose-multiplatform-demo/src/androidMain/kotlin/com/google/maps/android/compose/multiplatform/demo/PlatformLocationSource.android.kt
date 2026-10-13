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
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.maps.android.location.LocationPriority
import com.google.maps.android.location.LocationSource
import com.google.maps.android.location.asLocationSource
import com.google.maps.android.location.coarseLocationEvents
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.merge

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

private fun Context.hasAnyLocationPermission(): Boolean =
    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun Context.hasFineLocationPermission(): Boolean =
    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun Activity.shouldShowLocationRationale(): Boolean =
    shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
        shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)

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
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember { mutableStateOf(appContext.hasAnyLocationPermission()) }

    // Re-check location permission whenever the host lifecycle resumes (covering both the
    // system permission dialog closing and returning from Android App Settings).
    DisposableEffect(lifecycleOwner, appContext) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = appContext.hasAnyLocationPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // TODO(https://github.com/googlemaps/android-maps-utils/issues/1818): Once
    //  LocationManager.asLocationSource() handles cached lastKnownLocation emission and
    //  coarse-permission/indoor fallback directly in android-maps-utils, simplify this to
    //  `remember(manager) { manager.asLocationSource() }` matching PlatformLocationSource.ios.kt.
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
                        ).catch { if (it !is SecurityException) throw it },
                        manager.coarseLocationEvents(
                            minTimeMs = intervalMs,
                            minDistanceM = minUpdateDistanceM,
                        ).catch { if (it !is SecurityException) throw it },
                    )
                } else {
                    manager.coarseLocationEvents(
                        minTimeMs = intervalMs,
                        minDistanceM = minUpdateDistanceM,
                    ).catch { if (it !is SecurityException) throw it }
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

    var rationaleBeforeRequest by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions.values.any { it } || appContext.hasAnyLocationPermission()
        hasPermission = granted
        if (!granted) {
            val rationaleAfterRequest = activity?.shouldShowLocationRationale() ?: false
            // If shouldShowRequestPermissionRationale was false before the request and remains
            // false after the request was denied, Android suppressed the permission dialog
            // because the permission is permanently denied ("Don't ask again" / USER_FIXED).
            if (!rationaleBeforeRequest && !rationaleAfterRequest) {
                openAppSettings()
            }
        }
    }

    val requestPermission: () -> Unit = remember(activity, permissionLauncher, openAppSettings) {
        {
            if (activity == null) {
                openAppSettings()
            } else {
                rationaleBeforeRequest = activity.shouldShowLocationRationale()
                permissionLauncher.launch(LOCATION_PERMISSIONS)
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
