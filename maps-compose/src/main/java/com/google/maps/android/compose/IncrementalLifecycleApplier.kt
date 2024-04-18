package com.google.maps.android.compose

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.MapView

/** Invokes lifecycle events on the [lifecycleApplier] based on the current [lifecycle]. */
internal open class IncrementalLifecycleApplier(
    private val lifecycle: Lifecycle,
    private val lifecycleApplier: LifecycleApplier
) {
    /** The [Lifecycle.State] which was last applied to [lifecycleApplier] */
    protected var appliedLifecycleState = Lifecycle.State.INITIALIZED
    /** The [Lifecycle.Event] which was last received by [lifecycleEventObserver] */
    protected var lastObservedLifecycleEvent: Lifecycle.Event? = null
    private lateinit var lifecycleEventObserver: LifecycleEventObserver

    init {
        observeLifecycleEvents()
    }

    private fun observeLifecycleEvents() {
        lifecycleEventObserver = LifecycleEventObserver { _, event ->
            lastObservedLifecycleEvent = event
            onLifecycleEvent(event)
        }
        lifecycle.addObserver(lifecycleEventObserver)
    }

    protected fun onLifecycleEvent(lifecycleEvent: Lifecycle.Event) {
        val targetState = lifecycleEvent.targetState

        if (targetState == appliedLifecycleState) return

        val isValidTargetLifecycleEvent = isValidTargetLifecycleEvent(
            sourceLifecycleState = appliedLifecycleState,
            targetLifecycleEvent = lifecycleEvent
        )
        if (!isValidTargetLifecycleEvent) return

        moveToLifecycleEvent(lifecycleEvent)
    }

    private fun moveToLifecycleEvent(targetLifecycleEvent: Lifecycle.Event) {
        val direction = getLifecycleDirection(appliedLifecycleState, targetLifecycleEvent.targetState)

        do {
            val nextEvent = getNextLifecycleEvent(appliedLifecycleState, direction)!!
            invokeLifecycleEvent(nextEvent)
        } while (nextEvent != targetLifecycleEvent)
    }

    private fun invokeLifecycleEvent(lifecycleEvent: Lifecycle.Event) {
        lifecycleApplier.invokeEvent(lifecycleEvent)
        appliedLifecycleState = lifecycleEvent.targetState
    }

    fun destroyAndDispose() {
        Log.d(TAG, "destroyAndDispose()")
        onLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        lifecycle.removeObserver(lifecycleEventObserver)
    }

    protected companion object {

        /** @return if there is a valid path from [sourceLifecycleState] to [targetLifecycleEvent]. */
        fun isValidTargetLifecycleEvent(
            sourceLifecycleState: Lifecycle.State,
            targetLifecycleEvent: Lifecycle.Event
        ): Boolean {
            val direction = getLifecycleDirection(sourceLifecycleState, targetLifecycleEvent.targetState)
            var nextLifecycleEvent: Lifecycle.Event? = getNextLifecycleEvent(sourceLifecycleState, direction)

            while (true) {
                if (nextLifecycleEvent == null) return false
                if (nextLifecycleEvent == targetLifecycleEvent) return true
                nextLifecycleEvent = getNextLifecycleEvent(nextLifecycleEvent.targetState, direction)
            }
        }

        fun getLifecycleDirection(
            sourceLifecycleState: Lifecycle.State,
            targetLifecycleState: Lifecycle.State
        ) = when {
            targetLifecycleState.isAtLeast(sourceLifecycleState) -> LifecycleEventDirection.UP
            else -> LifecycleEventDirection.DOWN
        }

        fun getNextLifecycleEvent(from: Lifecycle.State, direction: LifecycleEventDirection) = when (direction) {
            LifecycleEventDirection.UP -> Lifecycle.Event.upFrom(from)
            LifecycleEventDirection.DOWN -> Lifecycle.Event.downFrom(from)
        }
    }

    protected enum class LifecycleEventDirection { UP, DOWN }
}

/** Controls a MapView's lifecycle + allows override of actual [lifecycle]'s value */
internal class IncrementalMapLifecycleApplier(
    lifecycle: Lifecycle,
    mapView: MapView
) : IncrementalLifecycleApplier(
    lifecycle = lifecycle,
    lifecycleApplier = MapViewLifecycleApplier(mapView)
) {
    /** Currently overwritten lifecycle state */
    private var temporaryLifecycleState: Lifecycle.State? = null

    fun setTemporaryLifecycleState(targetLifecycleState: Lifecycle.State) {
        if(targetLifecycleState in listOf(Lifecycle.State.DESTROYED, Lifecycle.State.INITIALIZED))
            error("Invalid temporary lifecycle state: $targetLifecycleState")

        val targetLifecycleEvent = getTargetLifecycleEventForState(
            appliedLifecycleState, targetLifecycleState
        ) ?: return

        temporaryLifecycleState = targetLifecycleState
        onLifecycleEvent(targetLifecycleEvent)
    }

    fun clearTemporaryLifecycleState() {
        temporaryLifecycleState ?: return
        lastObservedLifecycleEvent?.let { onLifecycleEvent(it) }
        temporaryLifecycleState = null
    }

    private companion object {
        fun getTargetLifecycleEventForState(
            currentLifecycleState: Lifecycle.State,
            targetLifecycleState: Lifecycle.State
        ): Lifecycle.Event? {
            val direction = getLifecycleDirection(currentLifecycleState, targetLifecycleState)

            return when(direction) {
                LifecycleEventDirection.UP -> Lifecycle.Event.upTo(targetLifecycleState)
                LifecycleEventDirection.DOWN -> Lifecycle.Event.downTo(targetLifecycleState)
            }
        }
    }
}

internal interface LifecycleApplier {
    fun invokeEvent(event: Lifecycle.Event)
}

private class MapViewLifecycleApplier(private val mapView: MapView) : LifecycleApplier {
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
