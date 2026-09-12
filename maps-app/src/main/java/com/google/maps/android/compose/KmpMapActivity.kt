/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.maps.android.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.google.maps.android.compose.multiplatform.GoogleMap
import com.google.maps.android.compose.multiplatform.MapMarker

class KmpMapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Renders the multiplatform Map Composable
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                latitude = 37.7749, // San Francisco
                longitude = -122.4194,
                zoom = 12f,
                markers = listOf(
                    MapMarker(
                        latitude = 37.7749,
                        longitude = -122.4194,
                        title = "San Francisco",
                        snippet = "Welcome to SF!"
                    )
                )
            )
        }
    }
}

