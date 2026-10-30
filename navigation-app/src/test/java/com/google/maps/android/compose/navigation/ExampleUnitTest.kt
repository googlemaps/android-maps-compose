package com.google.maps.android.compose.navigation

import android.app.Activity
import android.content.ComponentCallbacks2
import android.os.Bundle
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.libraries.navigation.NavigationView
import com.google.android.maps.robolectric.annotation.EnableMapsShadows
import com.google.common.truth.Truth.assertThat
import com.google.maps.android.compose.navigation.shadows.ShadowNavBitmapDescriptorFactory
import com.google.maps.android.compose.navigation.shadows.ShadowNavCameraUpdateFactory
import com.google.maps.android.compose.navigation.shadows.ShadowNavGoogleMap
import com.google.maps.android.compose.navigation.shadows.ShadowNavigationView
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    shadows = [
        ShadowNavigationView::class,
        ShadowNavGoogleMap::class,
        ShadowNavCameraUpdateFactory::class,
        ShadowNavBitmapDescriptorFactory::class,
    ],
)
@EnableMapsShadows
class ExampleUnitTest {

    @Test
    fun navigationViewDelegate_forwardsLifecycleAndAwaitsMap() = runTest {
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        val options = GoogleMapOptions().compassEnabled(true)
        val navigationView = NavigationView(activity, options)
        val shadowNavView = Shadow.extract(navigationView) as ShadowNavigationView
        val delegate = NavigationViewDelegate(navigationView)
        val savedInstanceState = Bundle()

        assertThat(delegate.mapView).isSameInstanceAs(navigationView)
        assertThat(shadowNavView.googleMapOptions).isSameInstanceAs(options)

        delegate.onCreate(savedInstanceState)
        assertThat(shadowNavView.isOnCreateCalled).isTrue()

        delegate.onStart()
        assertThat(shadowNavView.isOnStartCalled).isTrue()

        delegate.onResume()
        assertThat(shadowNavView.isOnResumeCalled).isTrue()

        val googleMap = delegate.awaitMap()
        assertThat(googleMap).isNotNull()
        assertThat(shadowNavView.googleMap).isSameInstanceAs(googleMap)

        delegate.onLowMemory()
        assertThat(shadowNavView.lastTrimMemoryLevel).isEqualTo(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)

        delegate.onPause()
        assertThat(shadowNavView.isOnPauseCalled).isTrue()

        delegate.onStop()
        assertThat(shadowNavView.isOnStopCalled).isTrue()

        delegate.onDestroy()
        assertThat(shadowNavView.isOnDestroyCalled).isTrue()
    }
}
