package com.google.maps.android.compose

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.MapView

internal class MapViewLifecycleController(
    isMapReused: Boolean,
    private val mapView: MapView
) {
    private val lcTag = run {
        val mapViewId = mapView.getTag(R.id.maps_compose_map_view_tag_debug_id)
        "MVLC/$mapViewId"
    }

    private companion object {
        /**
         * Used to navigate up/down through lifecycle state.
         * [up] and [down] are nullable suppliers to avoid circular references.
         *
         * Up means if a lifecycle goes "upwards", ie onCreate -> onResume.
         * Down means the opposite, ie onResume -> onDestroy.
         *
         * https://developer.android.com/guide/components/activities/activity-lifecycle#alc
         * */
        sealed class LifecycleStateNavigation(
            val lifecycle: Lifecycle.Event,
            val up: (() -> LifecycleStateNavigation)?,
            val down: (() -> LifecycleStateNavigation)?
        ) {
            companion object {
                private val instances = listOf(OnDestroy, OnStop, OnPause, OnCreate, OnStart, OnResume)
                fun forLifecycleEvent(lifecycleEvent: Lifecycle.Event) = instances.first { it.lifecycle == lifecycleEvent }
            }
        }

        val lifecycleUp = listOf(
            Lifecycle.Event.ON_CREATE,
            Lifecycle.Event.ON_START,
            Lifecycle.Event.ON_RESUME
        )

        data object OnDestroy: LifecycleStateNavigation(Lifecycle.Event.ON_DESTROY, null, null)
        data object OnStop: LifecycleStateNavigation(Lifecycle.Event.ON_STOP, { OnStart }, { OnDestroy })
        data object OnPause: LifecycleStateNavigation(Lifecycle.Event.ON_PAUSE, { OnResume }, { OnStop })
        data object OnCreate: LifecycleStateNavigation(Lifecycle.Event.ON_CREATE, { OnStart }, null)
        data object OnStart: LifecycleStateNavigation(Lifecycle.Event.ON_START, { OnResume }, null)
        data object OnResume: LifecycleStateNavigation(Lifecycle.Event.ON_RESUME, null, { OnPause })
    }

    private var created = isMapReused

    private var previousLifecycleState = if (isMapReused)
        Lifecycle.Event.ON_STOP else
        Lifecycle.Event.ON_CREATE

    var lifecycle: Lifecycle? = null
        set(value) {
            if (field !== value) {
                field?.removeObserver(observer)
                value?.addObserver(observer)
                field = value
            }
        }

    fun onDestroy() {
        lifecycle = null
        moveToLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    fun onLifecycleDetached() {
        lifecycle = null
        moveToLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    /**
     * Move to the lifecycle event instead of setting it directly.
     *
     *
     * */
    private fun moveToLifecycleEvent(targetLifecycle: Lifecycle.Event) {
        if(targetLifecycle == previousLifecycleState) return

        val moveUp = targetLifecycle in lifecycleUp
        val strDirection = if(moveUp) "UP" else "DOWN"
        var currentLifecycleNavigator = LifecycleStateNavigation.forLifecycleEvent(previousLifecycleState)
        Log.d(lcTag, "Moving $strDirection from ${currentLifecycleNavigator.lifecycle} to $targetLifecycle.")

        while(currentLifecycleNavigator.lifecycle != targetLifecycle) {
            buildString {
                appendLine("==========================================")
                appendLine("Current navigator: ${currentLifecycleNavigator.lifecycle}")
                appendLine("up: ${currentLifecycleNavigator.up?.invoke()?.lifecycle}")
                appendLine("down: ${currentLifecycleNavigator.down?.invoke()?.lifecycle}")
                appendLine("Moving: $strDirection")
                appendLine("------------------------------------------")
            }.also {
                Log.d(lcTag, it)
            }
            currentLifecycleNavigator = if(moveUp)
                currentLifecycleNavigator.up!!.invoke() else
                currentLifecycleNavigator.down!!.invoke()

            setLifecycleEvent(currentLifecycleNavigator.lifecycle)
        }
    }

    private fun setLifecycleEvent(event: Lifecycle.Event) {
        Log.d(lcTag, "Invoking: $event!")

        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                // Skip calling mapView.onCreate if the lifecycle did not go through onDestroy - in
                // this case the GoogleMap composable also doesn't leave the composition. So,
                // recreating the map does not restore state properly which must be avoided.
                if (previousLifecycleState != Lifecycle.Event.ON_STOP) {
                    mapView.onCreate(Bundle())
                }
            }

            Lifecycle.Event.ON_START -> mapView.onStart()
            Lifecycle.Event.ON_RESUME -> mapView.onResume()
            Lifecycle.Event.ON_PAUSE -> mapView.onPause()
            Lifecycle.Event.ON_STOP -> mapView.onStop()
            Lifecycle.Event.ON_DESTROY -> {
                // Handled in AndroidView onRelease
            }

            else -> throw IllegalStateException()
        }
        previousLifecycleState = event
    }

    private val observer = LifecycleEventObserver { _, event ->
        Log.d(lcTag, "---===[ LEO: Lifecycle event received from LifecycleEventObserver: $event. ]===---")

        if(!created) {
            Log.d(lcTag, "LEO: Invoking initial ON_CREATE.")
            created = true
            setLifecycleEvent(Lifecycle.Event.ON_CREATE)
        } else if(event == Lifecycle.Event.ON_CREATE) {
            Log.d(lcTag, "LEO: ON_CREATE lifecycle event was received but view is already created.")
        }

        if(event != Lifecycle.Event.ON_CREATE) {
            moveToLifecycleEvent(event)
        }
    }
}
