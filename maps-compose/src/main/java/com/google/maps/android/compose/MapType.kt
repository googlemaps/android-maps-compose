// Copyright 2021 Google LLC
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

/**
 * Enumerates the different types of map tiles.
 */
@Immutable
public enum class MapType(public val value: Int) {
    NONE(com.google.android.gms.maps.GoogleMap.MAP_TYPE_NONE),
    NORMAL(com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL),
    SATELLITE(com.google.android.gms.maps.GoogleMap.MAP_TYPE_SATELLITE),
    TERRAIN(com.google.android.gms.maps.GoogleMap.MAP_TYPE_TERRAIN),
    HYBRID(com.google.android.gms.maps.GoogleMap.MAP_TYPE_HYBRID)
}
