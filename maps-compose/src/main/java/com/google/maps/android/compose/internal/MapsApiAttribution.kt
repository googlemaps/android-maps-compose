
package com.google.maps.android.compose.internal

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.google.android.gms.maps.MapsApiSettings
import com.google.maps.android.compose.meta.AttributionId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Internal singleton to ensure that the Maps API attribution ID is added only once.
 */
internal object MapsApiAttribution {

    private val hasBeenCalled = AtomicBoolean(false)

    private val _isInitialized = mutableStateOf(false)
    val isInitialized: State<Boolean> = _isInitialized

    /**
     * Adds the attribution ID to the Maps API settings. This is done on a background thread
     * using [Dispatchers.IO]. The attribution ID is only added once.
     *
     * @param context The context to use to add the attribution ID.
     */
    fun addAttributionId(context: Context) {
        if (hasBeenCalled.compareAndSet(false, true)) {
            CoroutineScope(Dispatchers.IO).launch {
                MapsApiSettings.addInternalUsageAttributionId(context, AttributionId.VALUE)
                _isInitialized.value = true
            }
        }
    }
}
