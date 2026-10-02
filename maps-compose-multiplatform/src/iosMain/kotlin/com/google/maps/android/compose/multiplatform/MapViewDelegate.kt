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

import cocoapods.GoogleMaps.GMSCameraPosition
import cocoapods.GoogleMaps.GMSMapView
import cocoapods.GoogleMaps.GMSMapViewDelegateProtocol
import cocoapods.GoogleMaps.GMSMarker
import cocoapods.GoogleMaps.GMSOverlay
import cocoapods.GoogleMaps.animateToCameraPosition
import com.google.maps.android.model.CameraPosition
import com.google.maps.android.model.LatLng
import kotlin.coroutines.resume
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLLocationCoordinate2D
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.QuartzCore.CATransaction
import platform.darwin.NSObject

/** Forwards the map's events to the camera state, the drawn nodes and the map's callbacks. */
@OptIn(ExperimentalForeignApi::class)
internal class MapViewDelegate(
    private val cameraPositionState: CameraPositionState,
    private val renderer: MapNodeRenderer,
    private val onMapClick: (LatLng) -> Unit,
    private val onMapLongClick: (LatLng) -> Unit,
    private val onMapLoaded: () -> Unit,
) : NSObject(), GMSMapViewDelegateProtocol {
    private var loaded = false

    @ObjCSignatureOverride
    override fun mapView(mapView: GMSMapView, didChangeCameraPosition: GMSCameraPosition) {
        cameraPositionState.onCameraChanged(didChangeCameraPosition.toShared(), isMoving = true)
    }

    @ObjCSignatureOverride
    override fun mapView(mapView: GMSMapView, idleAtCameraPosition: GMSCameraPosition) {
        cameraPositionState.onCameraChanged(idleAtCameraPosition.toShared(), isMoving = false)
    }

    @ObjCSignatureOverride
    override fun mapView(mapView: GMSMapView, didTapAtCoordinate: CValue<CLLocationCoordinate2D>) {
        onMapClick(didTapAtCoordinate.toLatLng())
    }

    @ObjCSignatureOverride
    override fun mapView(mapView: GMSMapView, didLongPressAtCoordinate: CValue<CLLocationCoordinate2D>) {
        onMapLongClick(didLongPressAtCoordinate.toLatLng())
    }

    @ObjCSignatureOverride
    override fun mapView(mapView: GMSMapView, didTapMarker: GMSMarker): Boolean {
        val node = renderer.nodeFor(didTapMarker) as? MarkerNode ?: return false
        return node.onClick(node.state)
    }

    @ObjCSignatureOverride
    override fun mapView(mapView: GMSMapView, didDragMarker: GMSMarker) {
        renderer.onMarkerDragged(didDragMarker)
    }

    @ObjCSignatureOverride
    override fun mapView(mapView: GMSMapView, didEndDraggingMarker: GMSMarker) {
        renderer.onMarkerDragged(didEndDraggingMarker)
    }

    @ObjCSignatureOverride
    override fun mapView(mapView: GMSMapView, didTapOverlay: GMSOverlay) {
        when (val node = renderer.nodeFor(didTapOverlay)) {
            is PolylineNode -> node.onClick()
            is PolygonNode -> node.onClick()
            is CircleNode -> node.onClick()
            else -> Unit
        }
    }

    override fun mapViewDidFinishTileRendering(mapView: GMSMapView) {
        // Tiles finish rendering after every camera move; report only the first time.
        if (!loaded) {
            loaded = true
            onMapLoaded()
        }
    }
}

/** Moves a [GMSMapView]'s camera for a [CameraPositionState]. */
@OptIn(ExperimentalForeignApi::class)
internal class MapViewCameraController(private val mapView: GMSMapView) : CameraController {
    override fun move(position: CameraPosition) {
        mapView.camera = position.toGms()
    }

    override suspend fun animate(position: CameraPosition, durationMs: Int) {
        suspendCancellableCoroutine { continuation ->
            CATransaction.begin()
            CATransaction.setAnimationDuration(durationMs / 1_000.0)
            CATransaction.setCompletionBlock {
                if (continuation.isActive) continuation.resume(Unit)
            }
            mapView.animateToCameraPosition(position.toGms())
            CATransaction.commit()
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun LatLng.toCoordinate(): CValue<CLLocationCoordinate2D> =
    CLLocationCoordinate2DMake(latitude, longitude)

@OptIn(ExperimentalForeignApi::class)
internal fun CValue<CLLocationCoordinate2D>.toLatLng(): LatLng = useContents { LatLng(latitude, longitude) }
