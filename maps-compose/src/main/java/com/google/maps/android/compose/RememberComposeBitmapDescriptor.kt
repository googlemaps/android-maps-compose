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

import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.applyCanvas
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.core.graphics.createBitmap

@MapsComposeExperimentalApi
@Composable
public fun rememberComposeBitmapDescriptor(
    vararg keys: Any,
    content: @Composable () -> Unit,
): BitmapDescriptor {
    val parent = LocalView.current as ViewGroup
    val compositionContext = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)

    return remember(parent, compositionContext, currentContent, *keys) {
        renderComposableToBitmapDescriptor(parent, compositionContext, currentContent)
    }
}

private val measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
private val fakeCanvas = Canvas()

private fun renderComposableToBitmapDescriptor(
    parent: ViewGroup,
    compositionContext: CompositionContext,
    content: @Composable () -> Unit,
): BitmapDescriptor {
    // Host the throwaway rendering ComposeView on the window's root view rather than on `parent`
    // directly. `parent` (LocalView.current) can itself be mid-attach with no Android layout pass
    // performed on it yet -- e.g. when this is called from content composed inside a Clustering
    // item, which is first composed while its own hosting view is still being attached to its
    // parent. `setParentCompositionContext` keeps this composition correctly scoped to the
    // surrounding composition regardless of which Android View it's physically parented under, so
    // it's safe to use a different, already-laid-out ViewGroup as the Android host.
    val host = parent.rootView as? ViewGroup ?: parent

    val composeView =
        ComposeView(host.context)
            .apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                setParentCompositionContext(compositionContext)
                setContent(content)
            }
            .also(host::addView)

    // AndroidComposeView triggers LayoutNode's layout phase in the View draw phase, so trigger a
    // draw to an empty canvas to force that.
    composeView.draw(fakeCanvas)

    composeView.measure(measureSpec, measureSpec)

    if (composeView.measuredWidth == 0 || composeView.measuredHeight == 0) {
        throw IllegalStateException("The ComposeView was measured to have a width or height of " +
                "zero. Make sure that the content has a non-zero size.")
    }

    composeView.layout(0, 0, composeView.measuredWidth, composeView.measuredHeight)

    val bitmap =
        createBitmap(composeView.measuredWidth, composeView.measuredHeight)

    bitmap.applyCanvas { composeView.draw(this) }

    host.removeView(composeView)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
