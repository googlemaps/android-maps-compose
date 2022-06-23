// Copyright 2022 Google LLC
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

import androidx.compose.runtime.Immutable
import com.google.maps.android.compose.CameraMoveStartedReason.Companion.fromInt
import com.google.maps.android.compose.CameraMoveStartedReason.NO_MOVEMENT_YET
import com.google.maps.android.compose.CameraMoveStartedReason.UNKNOWN

/**
 * Enumerates the different reasons why the map camera started to move.
 *
 * Based on enum values from https://developers.google.com/android/reference/com/google/android/gms/maps/GoogleMap.OnCameraMoveStartedListener#constants.
 *
 * [NO_MOVEMENT_YET] is used as the initial state before any map movement has been observed.
 *
 * [UNKNOWN] is used to represent when an unsupported integer value is provided to [fromInt] - this
 * may be a new constant value from the Maps SDK that isn't supported by maps-compose yet, in which
 * case this library should be updated to include a new enum value for that constant.
 */
@Immutable
public enum class CameraMoveStartedReason(public val value: Int) {
    UNKNOWN(-2),
    NO_MOVEMENT_YET(-1),
    GESTURE(com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE),
    API_ANIMATION(com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener.REASON_API_ANIMATION),
    DEVELOPER_ANIMATION(com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION);

    public companion object {
        /**
         * Converts from the Maps SDK [com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener]
         * constants to [CameraMoveStartedReason], or returns [UNKNOWN] if there is no such
         * [CameraMoveStartedReason] for the given [value].
         *
         * See https://developers.google.com/android/reference/com/google/android/gms/maps/GoogleMap.OnCameraMoveStartedListener#constants.
         */
        public fun fromInt(value: Int): CameraMoveStartedReason {
            return values().firstOrNull { it.value == value } ?: return UNKNOWN
        }
    }
}