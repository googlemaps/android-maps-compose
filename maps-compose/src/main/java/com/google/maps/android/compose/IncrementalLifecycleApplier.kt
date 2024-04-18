package com.google.maps.android.compose

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.MapView

/** Invokes lifecycle events on the [lifecycleApplier] based on the current [lifecycle]. */
internal class IncrementalLifecycleApplier(
    private val lifecycle: Lifecycle,
    private var currentLifecycleState: Lifecycle.State,
    private val lifecycleApplier: LifecycleApplier
) {
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

    /** @return if there is a valid path from [sourceLifecycleState] to [targetLifecycleEvent]. */
    private fun isValidTargetLifecycleEvent(
        sourceLifecycleState: Lifecycle.State,
        targetLifecycleEvent: Lifecycle.Event
    ): Boolean {
        val direction = getLifecycleDirection(targetLifecycleEvent)
        var nextLifecycleEvent: Lifecycle.Event? =
            getNextLifecycleEvent(sourceLifecycleState, direction)

        while (true) {
            if (nextLifecycleEvent == null) return false
            if (nextLifecycleEvent == targetLifecycleEvent) return true
            nextLifecycleEvent = getNextLifecycleEvent(nextLifecycleEvent.targetState, direction)
        }
    }

    private fun getLifecycleDirection(targetLifecycleEvent: Lifecycle.Event) = when {
        targetLifecycleEvent.targetState.isAtLeast(currentLifecycleState) -> LifecycleEventDirection.UP
        else -> LifecycleEventDirection.DOWN
    }

    private fun getNextLifecycleEvent(from: Lifecycle.State, direction: LifecycleEventDirection) =
        when (direction) {
            LifecycleEventDirection.UP -> Lifecycle.Event.upFrom(from)
            LifecycleEventDirection.DOWN -> Lifecycle.Event.downFrom(from)
        }

    private fun moveToLifecycleEvent(targetLifecycleEvent: Lifecycle.Event) {
        val direction = getLifecycleDirection(targetLifecycleEvent)

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
        onLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        lifecycle.removeObserver(lifecycleEventObserver)
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

        mapView.tagData().lifecycleState = event.targetState
    }
}
