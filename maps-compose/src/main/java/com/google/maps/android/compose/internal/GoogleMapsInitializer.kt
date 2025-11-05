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
import android.os.StrictMode
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.MapsApiSettings
import com.google.maps.android.compose.meta.AttributionId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
public interface GoogleMapsInitializer {
    public val state: State<InitializationState>

    /**
     * The value of the attribution ID. Set this to the empty string to opt out of attribution.
     *
     * This must be set before calling the `initialize` function.
     */
    public var attributionId: String

    /**
     * Initializes Google Maps. This function must be called before using any other
     * functions in this library.
     *
     * If initialization fails with a recoverable error (e.g., a network issue),
     * the state will be reset to [InitializationState.UNINITIALIZED], allowing for a
     * subsequent retry. In the case of an unrecoverable error (e.g., a missing
     * manifest value), the state will be set to [InitializationState.FAILURE] and the
     * original exception will be re-thrown.
     *
     * @param context The context to use for initialization.
     * @param forceInitialization When true, initialization will be attempted even if it
     * has already succeeded or is in progress. This is useful for retrying a
     * previously failed initialization.
     */
    public suspend fun initialize(
        context: Context,
        forceInitialization: Boolean = false,
    )

    /**
     * Resets the initialization state.
     *
     * This function cancels any ongoing initialization and resets the state to `UNINITIALIZED`.
     * This is primarily useful in test environments where the SDK might need to be
     * re-initialized multiple times.
     */
    public suspend fun reset()
}

/**
 * The default implementation of [GoogleMapsInitializer].
 *
 * @param ioDispatcher The dispatcher to use for IO operations.
 */
public class DefaultGoogleMapsInitializer(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) : GoogleMapsInitializer {
    private val _state = mutableStateOf(InitializationState.UNINITIALIZED)
    override val state: State<InitializationState> = _state

    private val mutex = Mutex()

    override var attributionId: String = AttributionId.VALUE

    override suspend fun initialize(
        context: Context,
        forceInitialization: Boolean,
    ) {
        try {
            if (!forceInitialization &&
                (_state.value == InitializationState.INITIALIZING || _state.value == InitializationState.SUCCESS)
            ) {
                return
            }

            mutex.withLock {
                if (_state.value != InitializationState.UNINITIALIZED) {
                    return
                }
                _state.value = InitializationState.INITIALIZING
            }

            withContext(mainDispatcher) {
                val scope = this

                val policy = StrictMode.getThreadPolicy()
                try {
                    StrictMode.allowThreadDiskReads()
                    val result = MapsInitializer.initialize(context, null) {
                        scope.launch(ioDispatcher) {
                            MapsApiSettings.addInternalUsageAttributionId(context, attributionId)
                            _state.value = InitializationState.SUCCESS
                        }
                    }

                    if (result != ConnectionResult.SUCCESS) {
                        _state.value = InitializationState.FAILURE
                    }
                } finally {
                    StrictMode.setThreadPolicy(policy)
                }
            }
        } catch (e: com.google.android.gms.common.GooglePlayServicesMissingManifestValueException) {
            // This is an unrecoverable error. Play Services is not available (could be a test?)
            // Set the state to FAILURE to prevent further attempts.
            _state.value = InitializationState.FAILURE
            throw e
        } catch (e: Exception) {
            // This could be a transient error.
            // Reset to UNINITIALIZED to allow for a retry.
            _state.value = InitializationState.UNINITIALIZED
            throw e
        }
    }

    override suspend fun reset() {
        mutex.withLock {
            _state.value = InitializationState.UNINITIALIZED
        }
    }
}

/**
 * CompositionLocal that provides a [GoogleMapsInitializer].
 */
public val LocalGoogleMapsInitializer: ProvidableCompositionLocal<GoogleMapsInitializer> =
    compositionLocalOf {
        // Default implementation of the initializer
        DefaultGoogleMapsInitializer()
    }
