package com.google.maps.android.compose.navigation.shadows

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.UiSettings
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.libraries.navigation.NavigationView
import com.google.android.maps.robolectric.shadows.ShadowBitmapDescriptor
import com.google.android.maps.robolectric.shadows.ShadowBitmapDescriptorFactory
import com.google.android.maps.robolectric.shadows.ShadowCameraUpdate
import com.google.android.maps.robolectric.shadows.ShadowCameraUpdateFactory
import com.google.android.maps.robolectric.shadows.ShadowGoogleMap
import com.google.android.maps.robolectric.shadows.ShadowProjection
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.annotation.Resetter
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowViewGroup
import java.lang.reflect.Proxy

internal fun <T : Any> newInstanceWithDelegate(targetClass: Class<T>): T {
    try {
        val constructors = targetClass.declaredConstructors
        val ctor = constructors.firstOrNull {
            it.parameterTypes.size == 1 && it.parameterTypes[0].isInterface
        } ?: constructors.firstOrNull { it.parameterTypes.size == 1 }
        if (ctor != null) {
            ctor.isAccessible = true
            val delegateClass = ctor.parameterTypes[0]
            val dummyDelegate = if (delegateClass.isInterface) {
                Proxy.newProxyInstance(
                    delegateClass.classLoader,
                    arrayOf(delegateClass),
                ) { _, method, _ ->
                    when (method.returnType) {
                        Boolean::class.javaPrimitiveType -> false
                        Float::class.javaPrimitiveType -> 0f
                        Double::class.javaPrimitiveType -> 0.0
                        Long::class.javaPrimitiveType -> 0L
                        else -> if (method.returnType.isPrimitive) 0 else null
                    }
                }
            } else {
                Shadow.newInstanceOf(delegateClass)
            }
            return checkNotNull(targetClass.cast(ctor.newInstance(dummyDelegate)))
        }
    } catch (_: Throwable) {
    }
    return Shadow.newInstanceOf(targetClass)
}

@Suppress("FunctionName", "ktlint:standard:function-naming")
@Implements(NavigationView::class)
class ShadowNavigationView : ShadowViewGroup() {

    @RealObject
    private lateinit var realNavigationView: NavigationView

    var googleMapOptions: GoogleMapOptions? = null
        private set
    var googleMap: GoogleMap? = null
        private set

    var isOnCreateCalled: Boolean = false
        private set
    var isOnStartCalled: Boolean = false
        private set
    var isOnResumeCalled: Boolean = false
        private set
    var isOnPauseCalled: Boolean = false
        private set
    var isOnStopCalled: Boolean = false
        private set
    var isOnDestroyCalled: Boolean = false
        private set
    var lastTrimMemoryLevel: Int? = null
        private set

    @Implementation
    protected fun __constructor__(context: Context) {
        instances.add(realNavigationView)
    }

    @Implementation
    protected fun __constructor__(context: Context, options: GoogleMapOptions?) {
        this.googleMapOptions = options
        instances.add(realNavigationView)
    }

    @Implementation
    protected fun __constructor__(context: Context, attrs: AttributeSet?) {
        instances.add(realNavigationView)
    }

    @Implementation
    protected fun __constructor__(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        instances.add(realNavigationView)
    }

    @Implementation
    protected fun onCreate(savedInstanceState: Bundle?) {
        isOnCreateCalled = true
    }

    @Implementation
    protected fun onStart() {
        isOnStartCalled = true
    }

    @Implementation
    protected fun onResume() {
        isOnResumeCalled = true
    }

    @Implementation
    protected fun onPause() {
        isOnPauseCalled = true
    }

    @Implementation
    protected fun onStop() {
        isOnStopCalled = true
    }

    @Implementation
    protected fun onDestroy() {
        isOnDestroyCalled = true
    }

    @Implementation
    protected fun onTrimMemory(level: Int) {
        lastTrimMemoryLevel = level
    }

    @Implementation
    protected fun onSaveInstanceState(outState: Bundle) {
        // No-op for headless unit tests
    }

    @Implementation
    protected fun getMapAsync(callback: OnMapReadyCallback) {
        val map = googleMap ?: newInstanceWithDelegate(GoogleMap::class.java).also {
            googleMap = it
        }
        Handler(Looper.getMainLooper()).post {
            callback.onMapReady(map)
        }
    }

    companion object {
        val instances: MutableList<NavigationView> = mutableListOf()

        @Resetter
        @JvmStatic
        fun resetInstances() {
            instances.clear()
        }
    }
}

@Implements(GoogleMap::class)
class ShadowNavGoogleMap : ShadowGoogleMap() {
    private var navUiSettings: UiSettings? = null

    @Implementation
    override fun getProjection(): Projection {
        val projection = newInstanceWithDelegate(Projection::class.java)
        val shadow = Shadow.extract<ShadowProjection>(projection)
        shadow.setMapState(cameraPosition, viewportWidth, viewportHeight)
        return projection
    }

    @Implementation
    override fun getUiSettings(): UiSettings = navUiSettings ?: newInstanceWithDelegate(UiSettings::class.java).also {
        navUiSettings = it
    }
}

@Implements(CameraUpdateFactory::class)
class ShadowNavCameraUpdateFactory {
    companion object {
        private fun createUpdate(): CameraUpdate = newInstanceWithDelegate(CameraUpdate::class.java)

        @Implementation
        @JvmStatic
        fun newLatLng(latLng: LatLng): CameraUpdate {
            val update = createUpdate()
            val shadow = Shadow.extract<ShadowCameraUpdate>(update)
            shadow.latLng = latLng
            return update
        }

        @Implementation
        @JvmStatic
        fun newLatLngZoom(latLng: LatLng, zoom: Float): CameraUpdate {
            val update = createUpdate()
            val shadow = Shadow.extract<ShadowCameraUpdate>(update)
            shadow.latLng = latLng
            shadow.zoom = zoom
            return update
        }

        @Implementation
        @JvmStatic
        fun newCameraPosition(cameraPosition: CameraPosition): CameraUpdate {
            val update = createUpdate()
            val shadow = Shadow.extract<ShadowCameraUpdate>(update)
            shadow.cameraPosition = cameraPosition
            return update
        }

        @Implementation
        @JvmStatic
        fun zoomIn(): CameraUpdate = zoomBy(1.0f)

        @Implementation
        @JvmStatic
        fun zoomOut(): CameraUpdate = zoomBy(-1.0f)

        @Implementation
        @JvmStatic
        fun zoomTo(zoom: Float): CameraUpdate {
            val update = createUpdate()
            val shadow = Shadow.extract<ShadowCameraUpdate>(update)
            shadow.zoom = zoom
            return update
        }

        @Implementation
        @JvmStatic
        fun zoomBy(amount: Float): CameraUpdate {
            val update = createUpdate()
            val shadow = Shadow.extract<ShadowCameraUpdate>(update)
            shadow.zoomBy = amount
            return update
        }

        @Implementation
        @JvmStatic
        fun zoomBy(amount: Float, focus: Point): CameraUpdate = zoomBy(amount)

        @Implementation
        @JvmStatic
        fun newLatLngBounds(bounds: LatLngBounds, padding: Int): CameraUpdate {
            val update = createUpdate()
            val shadow = Shadow.extract<ShadowCameraUpdate>(update)
            shadow.setLatLngBounds(bounds, padding)
            return update
        }

        @Implementation
        @JvmStatic
        fun newLatLngBounds(bounds: LatLngBounds, width: Int, height: Int, padding: Int): CameraUpdate =
            newLatLngBounds(bounds, padding)

        @Implementation
        @JvmStatic
        fun scrollBy(xPixel: Float, yPixel: Float): CameraUpdate {
            val update = createUpdate()
            val shadow = Shadow.extract<ShadowCameraUpdate>(update)
            shadow.setScroll(xPixel, yPixel)
            return update
        }
    }
}

@Implements(BitmapDescriptorFactory::class)
class ShadowNavBitmapDescriptorFactory {
    companion object {
        private fun createDescriptor(bitmap: Bitmap?, identifier: String): BitmapDescriptor {
            val descriptor = newInstanceWithDelegate(BitmapDescriptor::class.java)
            val shadow = Shadow.extract<ShadowBitmapDescriptor>(descriptor)
            shadow.bitmap = bitmap
            shadow.identifier = identifier
            return descriptor
        }

        @Implementation
        @JvmStatic
        fun defaultMarker(): BitmapDescriptor = defaultMarker(ShadowBitmapDescriptorFactory.HUE_RED)

        @Implementation
        @JvmStatic
        fun defaultMarker(hue: Float): BitmapDescriptor = createDescriptor(null, "defaultMarker:hue=$hue")

        @Implementation
        @JvmStatic
        fun fromBitmap(image: Bitmap?): BitmapDescriptor {
            val id = if (image != null) "bitmap:${System.identityHashCode(image)}" else "bitmap:null"
            return createDescriptor(image, id)
        }

        @Implementation
        @JvmStatic
        fun fromResource(resourceId: Int): BitmapDescriptor {
            var bitmap: Bitmap? = null
            try {
                val app = RuntimeEnvironment.getApplication()
                if (app != null) {
                    bitmap = BitmapFactory.decodeResource(app.resources, resourceId)
                }
            } catch (_: Exception) {
            }
            return createDescriptor(bitmap, "resource:$resourceId")
        }

        @Implementation
        @JvmStatic
        fun fromAsset(assetName: String): BitmapDescriptor {
            var bitmap: Bitmap? = null
            try {
                val app = RuntimeEnvironment.getApplication()
                if (app != null) {
                    app.assets.open(assetName).use { stream ->
                        bitmap = BitmapFactory.decodeStream(stream)
                    }
                }
            } catch (_: Exception) {
            }
            return createDescriptor(bitmap, "asset:$assetName")
        }

        @Implementation
        @JvmStatic
        fun fromFile(fileName: String): BitmapDescriptor = fromPath(fileName)

        @Implementation
        @JvmStatic
        fun fromPath(absolutePath: String): BitmapDescriptor {
            var bitmap: Bitmap? = null
            try {
                bitmap = BitmapFactory.decodeFile(absolutePath)
            } catch (_: Exception) {
            }
            return createDescriptor(bitmap, "path:$absolutePath")
        }
    }
}

fun View.findNavigationView(): NavigationView? {
    if (this is NavigationView) return this
    if (this is ViewGroup) {
        for (i in 0 until childCount) {
            val found = getChildAt(i).findNavigationView()
            if (found != null) return found
        }
    }
    return null
}
