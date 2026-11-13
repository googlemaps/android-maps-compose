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

package com.google.maps.android.compose.navigation.shadows

import android.app.Activity
import android.app.Application
import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.navigation.NavigationApi.NavigatorListener
import com.google.android.libraries.navigation.Navigator
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.Resetter

@Implements(NavigationApi::class)
object ShadowNavigationApi {

    var lastListener: NavigatorListener? = null
        private set

    var navigatorToDeliver: Navigator? = null
    var errorCodeToDeliver: Int? = null

    @Implementation
    @JvmStatic
    fun getNavigator(activity: Activity, listener: NavigatorListener) {
        handleGetNavigator(listener)
    }

    @Implementation
    @JvmStatic
    fun getNavigator(application: Application, listener: NavigatorListener) {
        handleGetNavigator(listener)
    }

    private fun handleGetNavigator(listener: NavigatorListener) {
        lastListener = listener
        navigatorToDeliver?.let { listener.onNavigatorReady(it) }
        errorCodeToDeliver?.let { listener.onError(it) }
    }

    @Resetter
    @JvmStatic
    fun reset() {
        lastListener = null
        navigatorToDeliver = null
        errorCodeToDeliver = null
    }
}
