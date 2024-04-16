package com.google.maps.android.compose

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.MapView
import com.google.maps.android.compose.MapViewLifecycleController.LifecycleDirection.Down
import com.google.maps.android.compose.MapViewLifecycleController.LifecycleDirection.Up

internal class MapViewLifecycleController(
    isMapReused: Boolean,
    private val mapView: MapView
) {
    private val lcTag = run {
        val mapViewId = mapView.tagData().debugId
        "MVLC/$mapViewId"
    }

    private companion object {
        val lifecycleUp = listOf(
            Event.ON_CREATE,
            Event.ON_START,
            Event.ON_RESUME
        )
    }

    private enum class LifecycleDirection {
        Up, Down
    }

    private var previousLifecycleState = if (isMapReused)
        Event.ON_STOP else
        Event.ON_CREATE

    var lifecycle: Lifecycle? = null
        set(value) {
            if (field !== value) {
                field?.removeObserver(observer)
                value?.addObserver(observer)
                field = value
            }
        }

    /**
     * Move to the lifecycle event instead of setting it directly.
     * */
    private fun moveToLifecycleEvent(targetLifecycle: Event) {
        if (targetLifecycle == previousLifecycleState) return

        val lifecycleDirection = if (targetLifecycle in lifecycleUp) Up else Down

        (if (lifecycleDirection == Up) "UP" else "DOWN").let { strDirection ->
            Log.d(lcTag, "Moving $strDirection from $previousLifecycleState to $targetLifecycle.")
        }

        do {
            val nextLifecycleState = nextLifecycleEvent(lifecycleDirection)
            setLifecycleEvent(nextLifecycleState)
        } while (nextLifecycleState != targetLifecycle)
    }

    private fun nextLifecycleEvent(direction: LifecycleDirection) = when (previousLifecycleState) {
        Event.ON_CREATE -> when (direction) {
            Up -> Event.ON_START
            Down -> error("No lifecycle state below created.")
        }

        Event.ON_START -> when (direction) {
            Up -> Event.ON_RESUME
            Down -> error("No lifecycle state below started.")
        }

        Event.ON_RESUME -> when (direction) {
            Up -> error("No lifecycle state above resumed.")
            Down -> Event.ON_PAUSE
        }

        Event.ON_PAUSE -> when (direction) {
            Up -> Event.ON_RESUME
            Down -> Event.ON_STOP
        }

        Event.ON_STOP -> when (direction) {
            Up -> Event.ON_START
            Down -> Event.ON_DESTROY
        }

        Event.ON_ANY -> error("Unsupported operation")
        Event.ON_DESTROY -> error("No lifecycle state above destroyed")
    }


    private fun setLifecycleEvent(event: Event) {
        Log.d(lcTag, "Invoking: $event!")

        when (event) {
            Event.ON_CREATE -> {
                // Skip calling mapView.onCreate if the lifecycle did not go through onDestroy - in
                // this case the GoogleMap composable also doesn't leave the composition. So,
                // recreating the map does not restore state properly which must be avoided.
                if (previousLifecycleState != Event.ON_STOP) {
                    mapView.onCreate(Bundle())
                }
            }

            Event.ON_START -> mapView.onStart()
            Event.ON_RESUME -> mapView.onResume()
            Event.ON_PAUSE -> mapView.onPause()
            Event.ON_STOP -> mapView.onStop()
            Event.ON_DESTROY -> mapView.onDestroy()

            else -> throw IllegalStateException()
        }
        previousLifecycleState = event
    }

    private var created = isMapReused

    private val observer = LifecycleEventObserver { _, event ->
        Log.d(lcTag, "---===[ LEO: Lifecycle event received from LifecycleEventObserver: $event. ]===---")

        if (!created) {
            Log.d(lcTag, "LEO: Invoking initial ON_CREATE.")
            created = true
            setLifecycleEvent(Event.ON_CREATE)
        } else if (event == Event.ON_CREATE) {
            Log.d(lcTag, "LEO: ON_CREATE lifecycle event was received but view is already created.")
        }

        if (event != Event.ON_CREATE) {
            moveToLifecycleEvent(event)
        }
    }
}
