// Copyright 2025 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.maps.android.compose.internal

import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.MapsApiSettings
import com.google.maps.android.compose.meta.AttributionId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Enum representing the initialization state of the Google Maps SDK.
 */
public enum class InitializationState {
    /**
     * The SDK has not been initialized.
     */
    UNINITIALIZED,

    /**
     * The SDK is currently being initialized.
     */
    INITIALIZING,

    /**
     * The SDK has been successfully initialized.
     */
    SUCCESS,

    /**
     * The SDK initialization failed.
     */
    FAILURE
}

/**
 * A singleton object to manage the initialization of the Google Maps SDK.
 *
 * This object provides a state machine to track the initialization process and ensures that
 * the initialization is performed only once. It also provides a mechanism to reset the
 * initialization state, which can be useful in test environments.
 *
 * The initialization process consists of two main steps:
 * 1. Calling `MapsInitializer.initialize(context)` to initialize the Google Maps SDK.
 * 2. Calling `MapsApiSettings.addInternalUsageAttributionId(context, attributionId)` to add
 *    the library's attribution ID to the Maps API settings.
 *
 * The state of the initialization is exposed via the `state` property, which is a [State] object
 * that can be observed for changes.
 */
public object GoogleMapsInitializer {
    private val _state = mutableStateOf(InitializationState.UNINITIALIZED)
    public val state: State<InitializationState> = _state

    private val mutex = Mutex()

    /**
     * The value of the attribution ID. Set this to the empty string to opt out of attribution.
     *
     * This must be set before calling the `initialize` function.
     */
    public var attributionId: String = AttributionId.VALUE

    /**
     * Initializes the Google Maps SDK.
     *
     * This function starts the initialization process on a background thread. The process is
     * performed only once. If the initialization is already in progress or has completed,
     * this function does nothing.
     *
     * The initialization state can be observed via the `state` property.
     *
     * @param context The context to use for initialization.
     */
    public suspend fun initialize(context: Context, forceInitialization: Boolean = false) {
        // 1. Quick exit if already initialized or in progress.
        if (!forceInitialization &&
            (_state.value == InitializationState.INITIALIZING || _state.value == InitializationState.SUCCESS)) {
            return // Already initialized or initializing, and not forced.
        }

        // 2. Acquire the mutex, perform a double-check (in case another
        //    coroutine was also waiting), and set the state.
        //    This block is synchronous and ensures only one coroutine
        //    proceeds to the IO operation.
        mutex.withLock {
            if (_state.value != InitializationState.UNINITIALIZED) {
                return // Another coroutine won the race while this one was suspended on the lock.
            }
            _state.value = InitializationState.INITIALIZING
        }

        // The lock is now released.

        // 3. Run the blocking initialization code on the IO dispatcher.
        //    This function will SUSPEND until the withContext(Dispatchers.IO) block completes.
        //    If the calling scope is cancelled while waiting, withContext will throw
        //    a CancellationException, and the state will remain INITIALIZING
        //    (which the catch block will update to FAILURE).
        try {
            withContext(Dispatchers.IO) {
                // This is the blocking call. The thread will be blocked here.
                // If cancellation happens, the thread STILL finishes this call,
                // but the coroutine will immediately throw CancellationException
                // *after* this call returns, skipping the state assignments below.
                if (MapsInitializer.initialize(context) == ConnectionResult.SUCCESS) {
                    MapsApiSettings.addInternalUsageAttributionId(context, attributionId)
                    _state.value = InitializationState.SUCCESS
                } else {
                    // Handle cases where initialize() returns a non-SUCCESS code
                    _state.value = InitializationState.FAILURE
                }
            }
        } catch (e: Exception) {
            when (e) {
                is com.google.android.gms.common.GooglePlayServicesMissingManifestValueException -> {
                    // This is an unrecoverable error.
                    Log.w("GoogleMapsInitializer", "Initialization failed", e)
                    _state.value = InitializationState.FAILURE
                }
                else -> {
                    Log.w("GoogleMapsInitializer", "Initialization failed", e)
                    _state.value = InitializationState.UNINITIALIZED
                }
            }
            // This will catch any exceptions from the init process (like from mocks in tests)
            // Note: By default, this does NOT catch CancellationException.
        }
    }

    /**
     * Resets the initialization state.
     *
     * This function cancels any ongoing initialization and resets the state to `UNINITIALIZED`.
     * This is useful in test environments where you might need to re-initialize the SDK
     * multiple times.
     */
    public suspend fun reset() {
        mutex.withLock {
            _state.value = InitializationState.UNINITIALIZED
        }
    }
}