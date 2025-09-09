
package com.google.maps.android.compose

import com.google.android.gms.maps.model.LatLng
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertAbout

/**
 * A [Subject] for asserting facts about [LatLng] objects.
 */
class LatLngSubject(
    failureMetadata: FailureMetadata,
    private val actual: LatLng?
) : Subject(failureMetadata, actual) {

    /**
     * Asserts that the subject is equal to the given [expected] value, with a given [tolerance].
     */
    fun isEqualTo(expected: LatLng, tolerance: Double = 1e-6) {
        if (actual == null) {
            failWithActual("expected", expected)
            return
        }

        check("latitude").that(actual.latitude).isWithin(tolerance).of(expected.latitude)
        check("longitude").that(actual.longitude).isWithin(tolerance).of(expected.longitude)
    }

    companion object {
        /**
         * A factory for creating [LatLngSubject] instances.
         */
        fun assertThat(actual: LatLng?): LatLngSubject {
            return assertAbout(latLngs()).that(actual)
        }

        private fun latLngs(): (failureMetadata: FailureMetadata, actual: LatLng?) -> LatLngSubject {
            return ::LatLngSubject
        }
    }
}
