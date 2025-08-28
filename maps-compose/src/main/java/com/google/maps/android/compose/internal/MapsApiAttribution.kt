
package com.google.maps.android.compose.internal

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.google.android.gms.maps.MapsApiSettings
import com.google.maps.android.compose.meta.AttributionId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Internal singleton to ensure that the Maps API attribution ID is added only once.
 */
internal object MapsApiAttribution {

    private val hasBeenCalled = AtomicBoolean(false)

    private val _isInitialized = mutableStateOf(false)
    val isInitialized: State<Boolean> = _isInitialized

    /**
     * The value of the attribution ID.  Set this to the empty string to opt out of attribution.
     */
    var attributionId: String = AttributionId.VALUE

    /**
     * Adds the attribution ID to the Maps API settings. This is done on a background thread
     * using [Dispatchers.IO]. The attribution ID is only added once.
     *
     * Adds a usage attribution ID to the initializer, which helps Google understand which libraries
     * and samples are helpful to developers, such as usage of this library.
     * To opt out of sending the usage attribution ID, it is safe to delete this function call
     * or replace the value with an empty string.
     *
     * See https://developers.google.com/android/reference/com/google/android/gms/maps/MapsApiSettings#addInternalUsageAttributionId(android.content.Context,%20java.lang.String)
     *
     * @param context The context to use to add the attribution ID.
     */
    suspend fun addAttributionId(context: Context) {
        if (hasBeenCalled.compareAndSet(false, true)) {
            withContext(Dispatchers.IO) {
                MapsApiSettings.addInternalUsageAttributionId(context, attributionId)
                _isInitialized.value = true
            }
        }
    }
}
