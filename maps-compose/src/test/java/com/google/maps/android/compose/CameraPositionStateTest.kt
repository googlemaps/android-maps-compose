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

package com.google.maps.android.compose

import android.app.Activity
import android.os.Bundle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.maps.robolectric.annotation.EnableMapsShadows
import com.google.android.maps.robolectric.shadows.ShadowGoogleMap
import com.google.android.maps.robolectric.truth.LatLngSubject
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowLooper

// [START maps_compose_camera_position_state_test]
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@EnableMapsShadows
internal class CameraPositionStateTest {

    private lateinit var googleMap: GoogleMap
    private lateinit var shadowMap: ShadowGoogleMap

    private val sydney: LatLng = LatLng(-33.852, 151.211)
    private val tokyo: LatLng = LatLng(35.6762, 139.6503)

    @Before
    internal fun setUp(): Unit {
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        val mapView = MapView(activity)
        mapView.onCreate(Bundle())

        val mapRef = AtomicReference<GoogleMap>()
        mapView.getMapAsync { mapRef.set(it) }
        ShadowLooper.idleMainLooper()

        googleMap = mapRef.get()
        shadowMap = Shadow.extract(googleMap) as ShadowGoogleMap
    }

    @Test
    internal fun testInitialPosition_withoutMapBound(): Unit {
        val state = CameraPositionState(position = CameraPosition.fromLatLngZoom(sydney, 10f))
        assertThat(state.position.target).isEqualTo(sydney)
        assertThat(state.position.zoom).isEqualTo(10f)
        assertThat(state.isMoving).isFalse()
        assertThat(state.projection).isNull()
    }

    @Test
    internal fun testSetMap_syncsPositionFromStateToMap(): Unit {
        val initialPosition = CameraPosition.fromLatLngZoom(sydney, 12f)
        val state = CameraPositionState(position = initialPosition)

        state.setMap(googleMap)

        assertThat(googleMap.cameraPosition.target).isEqualTo(sydney)
        assertThat(googleMap.cameraPosition.zoom).isEqualTo(12f)
        assertThat(state.projection).isNotNull()
    }

    private fun bindState(state: CameraPositionState) {
        state.setMap(googleMap)
        googleMap.setOnCameraMoveStartedListener { reason ->
            state.cameraMoveStartedReason = CameraMoveStartedReason.fromInt(reason)
            state.isMoving = true
        }
        googleMap.setOnCameraMoveListener {
            state.rawPosition = googleMap.cameraPosition
        }
        googleMap.setOnCameraIdleListener {
            state.isMoving = false
            state.rawPosition = googleMap.cameraPosition
        }
    }

    @Test
    internal fun testMove_instantaneouslyUpdatesCameraPosition(): Unit {
        val state = CameraPositionState()
        bindState(state)

        state.move(CameraUpdateFactory.newLatLngZoom(tokyo, 14f))

        assertThat(googleMap.cameraPosition.target).isEqualTo(tokyo)
        LatLngSubject.assertThat(state.position.target)
            .isWithin(1.0)
            .of(tokyo)
        assertThat(state.position.zoom).isEqualTo(14f)
        assertThat(state.isMoving).isFalse()
    }

    @Test
    internal fun testAnimate_updatesCameraPosition(): Unit = runTest {
        val state = CameraPositionState()
        bindState(state)

        state.animate(CameraUpdateFactory.newLatLngZoom(tokyo, 15f), durationMs = 1000)

        assertThat(googleMap.cameraPosition.target).isEqualTo(tokyo)
        LatLngSubject.assertThat(state.position.target)
            .isWithin(1.0)
            .of(tokyo)
        assertThat(state.position.zoom).isEqualTo(15f)
        assertThat(state.isMoving).isFalse()
    }

    @Test
    internal fun testProjection_convertsCoordinates(): Unit {
        val state = CameraPositionState()
        state.setMap(googleMap)

        state.move(CameraUpdateFactory.newLatLngZoom(sydney, 10f))

        val projection = state.projection
        assertThat(projection).isNotNull()
        val screenPoint = projection?.toScreenLocation(sydney)
        assertThat(screenPoint).isNotNull()
        val roundTripLatLng = projection?.fromScreenLocation(screenPoint!!)
        LatLngSubject.assertThat(roundTripLatLng)
            .isWithin(1.0)
            .of(sydney)
    }
}
// [END maps_compose_camera_position_state_test]
