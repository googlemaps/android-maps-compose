package com.google.maps.android.compose.streetview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.android.gms.maps.StreetViewPanorama
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.StreetViewPanoramaCamera
import com.google.android.gms.maps.model.StreetViewPanoramaLocation
import com.google.android.gms.maps.model.StreetViewSource

@Composable
public inline fun rememberStreetViewCameraPositionState(
    crossinline init: StreetViewCameraPositionState.() -> Unit = {}
): StreetViewCameraPositionState = remember {
    StreetViewCameraPositionState().apply(init)
}

public class StreetViewCameraPositionState {

    /**
     * The location of the panorama.
     *
     * This is read-only - to update the camera's position use [setPosition].
     *
     * Note that this property is observable and if you use it in a composable function it will be
     * recomposed on every change. Use `snapshotFlow` to observe it instead.
     */
    public val location: StreetViewPanoramaLocation
        get() = rawLocation

    internal var rawLocation by mutableStateOf(StreetViewPanoramaLocation(arrayOf(), LatLng(0.0,0.0), ""))

    /**
     * The camera of the panorama.
     *
     * Note that this property is observable and if you use it in a composable function it will be
     * recomposed on every change. Use `snapshotFlow` to observe it instead.
     */
    public val panoramaCamera: StreetViewPanoramaCamera
        get() = rawPanoramaCamera

    internal var rawPanoramaCamera by mutableStateOf(StreetViewPanoramaCamera(0f, 0f, 0f ))

    internal var panorama: StreetViewPanorama? = null
        set(value) {
            // Set value
            if (field == null && value == null) return
            if (field != null && value != null) {
                error("StreetViewCameraPositionState may only be associated with one StreetView at a time.")
            }
            field = value
        }

    /**
     * Animates the camera to be at [camera] in [durationMs] milliseconds.
     * @param camera the camera to update to
     * @param durationMs the duration of the animation in milliseconds
     */
    public fun animateTo(camera: StreetViewPanoramaCamera, durationMs: Int) {
        panorama?.animateTo(camera, durationMs.toLong())
    }

    /**
     * Sets the position of the panorama.
     * @param position the LatLng of the panorama
     * @param radius the area in which to search for a panorama in meters
     * @param source the source of the panoramas
     */
    public fun setPosition(position: LatLng, radius: Int? = null, source: StreetViewSource? = null) {
        if (radius == null && source == null) {
            panorama?.setPosition(position)
        } else if (radius != null && source == null) {
            panorama?.setPosition(position, radius)
        } else if (radius != null) {
            panorama?.setPosition(position, radius, source)
        }
    }

    /**
     * Sets the StreetViewPanorama to the given panorama ID.
     * @param panoId the ID of the panorama to set to
     */
    public fun setPosition(panoId: String) {
        panorama?.setPosition(panoId)
    }
}