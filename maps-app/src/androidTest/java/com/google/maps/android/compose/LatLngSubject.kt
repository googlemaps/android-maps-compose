
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
