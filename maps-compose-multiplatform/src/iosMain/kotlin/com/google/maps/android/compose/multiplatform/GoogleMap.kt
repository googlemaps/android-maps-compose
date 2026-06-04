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

package com.google.maps.android.compose.multiplatform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.math.pow
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKCoordinateRegionMake
import platform.MapKit.MKCoordinateSpanMake
import platform.MapKit.MKMapView

@OptIn(ExperimentalForeignApi::class)
@Composable
public actual fun GoogleMap(
    modifier: Modifier,
    latitude: Double,
    longitude: Double,
    zoom: Float
) {
    UIKitView(
        factory = {
            MKMapView()
        },
        modifier = modifier,
        update = { mapView ->
            val center = CLLocationCoordinate2DMake(latitude, longitude)
            // Approximate span calculation based on zoom level
            val spanDelta = 360.0 / 2.0.pow(zoom.toDouble())
            val span = MKCoordinateSpanMake(spanDelta, spanDelta)
            val region = MKCoordinateRegionMake(center, span)
            mapView.setRegion(region, animated = true)
        }
    )
}
