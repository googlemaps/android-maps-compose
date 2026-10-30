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
