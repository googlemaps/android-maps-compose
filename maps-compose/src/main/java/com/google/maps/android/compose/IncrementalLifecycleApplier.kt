package com.google.maps.android.compose

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.MapView

/** Invokes lifecycle events on the [lifecycleApplier] based on the current [lifecycle]. */
internal class IncrementalLifecycleApplier(
    private val lifecycle: Lifecycle,
    private val lifecycleApplier: LifecycleApplier
) {
    private var currentLifecycleState = Lifecycle.State.INITIALIZED
    private lateinit var lifecycleEventObserver: LifecycleEventObserver

    init {
        observeLifecycleEvents()
    }

    private fun observeLifecycleEvents() {
        lifecycleEventObserver = LifecycleEventObserver { _, event -> onLifecycleEvent(event) }
        lifecycle.addObserver(lifecycleEventObserver)
    }

    private fun onLifecycleEvent(lifecycleEvent: Lifecycle.Event) {
        val targetState = lifecycleEvent.targetState

        if (targetState == currentLifecycleState) return

        val isValidTargetLifecycleEvent = isValidTargetLifecycleEvent(
            sourceLifecycleState = currentLifecycleState,
            targetLifecycleEvent = lifecycleEvent
        )
        if (!isValidTargetLifecycleEvent) return

        moveToLifecycleEvent(lifecycleEvent)
    }

    private fun moveToLifecycleEvent(targetLifecycleEvent: Lifecycle.Event) {
        val direction = getLifecycleDirection(currentLifecycleState, targetLifecycleEvent)

        do {
            val nextEvent = getNextLifecycleEvent(currentLifecycleState, direction)!!
            invokeLifecycleEvent(nextEvent)
        } while (nextEvent != targetLifecycleEvent)
    }

    private fun invokeLifecycleEvent(lifecycleEvent: Lifecycle.Event) {
        lifecycleApplier.invokeEvent(lifecycleEvent)
        currentLifecycleState = lifecycleEvent.targetState
    }

    fun destroyAndDispose() {
        Log.d(TAG, "destroyAndDispose()")
        onLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        lifecycle.removeObserver(lifecycleEventObserver)
    }

    private companion object {

        /** @return if there is a valid path from [sourceLifecycleState] to [targetLifecycleEvent]. */
        private fun isValidTargetLifecycleEvent(
            sourceLifecycleState: Lifecycle.State,
            targetLifecycleEvent: Lifecycle.Event
        ): Boolean {
            val direction = getLifecycleDirection(sourceLifecycleState, targetLifecycleEvent)
            var nextLifecycleEvent: Lifecycle.Event? = getNextLifecycleEvent(sourceLifecycleState, direction)

            while (true) {
                if (nextLifecycleEvent == null) return false
                if (nextLifecycleEvent == targetLifecycleEvent) return true
                nextLifecycleEvent = getNextLifecycleEvent(nextLifecycleEvent.targetState, direction)
            }
        }

        fun getLifecycleDirection(
            sourceLifecycleState: Lifecycle.State,
            targetLifecycleEvent: Lifecycle.Event
        ) = when {
            targetLifecycleEvent.targetState.isAtLeast(sourceLifecycleState) -> LifecycleEventDirection.UP
            else -> LifecycleEventDirection.DOWN
        }

        fun getNextLifecycleEvent(from: Lifecycle.State, direction: LifecycleEventDirection) = when (direction) {
            LifecycleEventDirection.UP -> Lifecycle.Event.upFrom(from)
            LifecycleEventDirection.DOWN -> Lifecycle.Event.downFrom(from)
        }
    }
}

private enum class LifecycleEventDirection { UP, DOWN }

internal interface LifecycleApplier {
    fun invokeEvent(event: Lifecycle.Event)
}

internal class MapViewLifecycleApplier(private val mapView: MapView) : LifecycleApplier {
    override fun invokeEvent(event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
            Lifecycle.Event.ON_START -> mapView.onStart()
            Lifecycle.Event.ON_RESUME -> mapView.onResume()
            Lifecycle.Event.ON_PAUSE -> mapView.onPause()
            Lifecycle.Event.ON_STOP -> mapView.onStop()
            Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
            Lifecycle.Event.ON_ANY -> error("Cannot invoke lifecycle event ON_ANY on mapView.")
        }

        Log.d("MapViewLifecycleApplier", "[MapView#${ mapView.tagData().debugId }]Invoking $event")

        mapView.tagData().lifecycleState = event.targetState
    }
}
