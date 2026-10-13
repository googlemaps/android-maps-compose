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
import androidx.compose.runtime.Stable
import com.google.maps.android.location.LocationSource

/**
 * Holds a multiplatform [LocationSource] alongside observable runtime permission state and
 * platform callbacks to request permission or open system application settings.
 *
 * Exposing permission state alongside [locationSource] allows shared `commonMain` UI to react
 * gracefully when the user denies or revokes location permission at runtime, rather than crashing
 * or hanging indefinitely.
 */
@Stable
internal class PlatformLocationController(
    val locationSource: LocationSource,
    val hasPermission: Boolean,
    val requestPermission: () -> Unit,
    val openAppSettings: () -> Unit,
)

/**
 * Remembers a platform-backed [PlatformLocationController] (`LocationManager` on Android,
 * `CLLocationManager` on iOS) that tracks runtime location permissions and streams device GPS
 * fixes into shared `commonMain` UI via [LocationSource.locationEvents].
 */
@Composable
internal expect fun rememberPlatformLocationController(): PlatformLocationController
