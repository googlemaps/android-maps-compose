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

import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.internal.ICameraUpdateFactoryDelegate
import com.google.android.gms.maps.internal.IGoogleMapDelegate
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * A call recorded by a fake Maps SDK delegate: the delegate method name and its arguments.
 * Wrapped arguments are unwrapped, so a camera update shows up as the [FakeCall] that created it.
 */
internal data class FakeCall(val method: String, val args: List<Any?>)

/**
 * Creates a proxy for the Maps SDK delegate interface [T] that records every call in [calls]
 * and returns [result] (or a default value) from it.
 */
internal inline fun <reified T> fakeDelegate(
    calls: MutableList<FakeCall> = mutableListOf(),
    noinline result: (Method, List<Any?>) -> Any? = { _, _ -> null },
): T = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, args ->
    val unwrapped = args.orEmpty().map { if (it is IObjectWrapper) it.unwrapFake() else it }
    calls += FakeCall(method.name, unwrapped)
    result(method, unwrapped) ?: defaultValue(method.returnType)
} as T

private fun defaultValue(type: Class<*>): Any? = when (type) {
    java.lang.Boolean.TYPE -> false
    java.lang.Float.TYPE -> 0f
    java.lang.Integer.TYPE -> 0
    java.lang.Long.TYPE -> 0L
    java.lang.Double.TYPE -> 0.0
    else -> null
}

private class FakeWrapperHandler(val value: Any?) : InvocationHandler {
    override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any? = null
}

/**
 * Wraps [value] in an [IObjectWrapper]. The SDK's own ObjectWrapper extends Binder, which is not
 * available in JVM unit tests.
 */
internal fun fakeWrapper(value: Any?): IObjectWrapper = Proxy.newProxyInstance(
    IObjectWrapper::class.java.classLoader,
    arrayOf(IObjectWrapper::class.java),
    FakeWrapperHandler(value),
) as IObjectWrapper

private fun IObjectWrapper.unwrapFake(): Any? = (Proxy.getInvocationHandler(this) as FakeWrapperHandler).value

/** Creates a [GoogleMap] whose delegate records its calls in [calls]. */
internal fun fakeGoogleMap(calls: MutableList<FakeCall>): GoogleMap = GoogleMap(fakeDelegate<IGoogleMapDelegate>(calls))

/**
 * Initializes [CameraUpdateFactory] with a fake delegate, so that each camera update wraps the
 * [FakeCall] that created it.
 */
internal fun initFakeCameraUpdateFactory() {
    val delegate = fakeDelegate<ICameraUpdateFactoryDelegate> { method, args ->
        fakeWrapper(FakeCall(method.name, args))
    }
    CameraUpdateFactory::class.java.declaredFields
        .single { it.type == ICameraUpdateFactoryDelegate::class.java }
        .apply { isAccessible = true }
        .set(null, delegate)
}
