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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.maps.android.location.asLocationSource
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.NSObject

private fun CLAuthorizationStatus.isAuthorized(): Boolean = this == kCLAuthorizationStatusAuthorizedWhenInUse ||
    this == kCLAuthorizationStatusAuthorizedAlways

@Composable
internal actual fun rememberPlatformLocationController(): PlatformLocationController {
    val manager = remember { CLLocationManager() }
    var hasPermission by remember { mutableStateOf(manager.authorizationStatus.isAuthorized()) }

    // Observe CoreLocation authorization changes and app foreground activation so returning
    // from the system permission dialog or iOS Settings updates hasPermission without polling.
    DisposableEffect(manager) {
        val authObserver = CLLocationManager()
        val authDelegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                hasPermission = manager.authorizationStatus.isAuthorized()
            }
        }
        authObserver.delegate = authDelegate
        val notificationObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationDidBecomeActiveNotification,
            `object` = null,
            queue = null,
        ) { _ ->
            hasPermission = manager.authorizationStatus.isAuthorized()
        }
        onDispose {
            // Keep authDelegate referenced until onDispose because CLLocationManager.delegate is weak.
            if (authObserver.delegate === authDelegate) {
                authObserver.delegate = null
            }
            NSNotificationCenter.defaultCenter.removeObserver(notificationObserver)
        }
    }

    val locationSource = remember(manager) { manager.asLocationSource() }

    val requestPermission: () -> Unit = remember(manager) {
        { manager.requestWhenInUseAuthorization() }
    }

    val openAppSettings: () -> Unit = remember {
        {
            val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
            if (url != null) {
                UIApplication.sharedApplication.openURL(
                    url,
                    options = emptyMap<Any?, Any>(),
                    completionHandler = null,
                )
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
