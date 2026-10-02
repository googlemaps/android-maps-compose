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
import com.google.maps.android.compose.multiplatform.demo.MultiplatformDemoApp

/**
 * The maps-compose-multiplatform demos, the same screens the iOS demo app shows. Opens on the
 * first demo, so that this entry shows a map like every other entry of the demo app, or on the
 * demo at the `demo` extra, for example
 * `adb shell am start -n com.google.maps.android.compose/.KmpMapActivity --ei demo 3`.
 */
class KmpMapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MultiplatformDemoApp(startDemo = intent.getIntExtra("demo", 0))
        }
    }
}
