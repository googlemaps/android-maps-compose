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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ComposeView
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import com.google.android.gms.maps.MapView
import java.io.Closeable

/**
 * Prepares [view] for a single render by temporarily attaching it as a descendant of this
 * [MapView].
 * This is a trick to enable [ComposeView] to start its composition, as it requires being attached
 * to a window. [onAddedToWindow] is called in place, and then [view] is removed from the window
 * before returning.
 */
internal fun MapView.renderComposeViewOnce(
    view: AbstractComposeView,
    onAddedToWindow: ((View) -> Unit)? = null,
    parentContext: CompositionContext,
) {
    startRenderingComposeView(view, parentContext).use {
        onAddedToWindow?.invoke(view)
    }
}

private val unspecifiedMeasureSpec =
    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

/**
 * Renders [content] into a standalone [Bitmap] by temporarily attaching a [ComposeView] as a
 * descendant of this [MapView], measuring, laying out and drawing it, then detaching it again.
 *
 * Unlike [startRenderingComposeView], the returned bitmap has no ties back to this [MapView] or
 * its composition once this function returns, so it is safe to hand off to APIs (such as
 * [com.google.android.gms.maps.GoogleMap.InfoWindowAdapter]) that take ownership of the view they
 * receive and re-parent it into their own hierarchy — which would otherwise crash with
 * "The specified child already has a parent" if handed a view still attached elsewhere.
 *
 * Returns `null` if this [MapView] is no longer attached to a window by the time [content] is
 * measured. That happens when the Maps SDK's own async render request (which drives this call)
 * lands after Compose has already unparented the [MapView] (e.g. `LazyColumn` recycling/detach):
 * the composition never gets a real layout pass, so it measures to zero size. There's no info
 * window worth rendering for a map that's already being torn down, so this is treated as "nothing
 * to show" rather than an error.
 */
internal fun MapView.renderComposableToBitmap(
    parentContext: CompositionContext,
    content: @Composable () -> Unit,
): Bitmap? {
    val containerView = ensureContainerView()
    val composeView = ComposeView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        setParentCompositionContext(parentContext)
        setContent(content)
    }
    containerView.addView(composeView)

    composeView.measure(unspecifiedMeasureSpec, unspecifiedMeasureSpec)
    val width = composeView.measuredWidth
    val height = composeView.measuredHeight

    if (width <= 0 || height <= 0) {
        containerView.removeView(composeView)
        check(!isAttachedToWindow) {
            "The info window content was measured to have a width or height of zero. " +
                "Make sure that the content has a non-zero size."
        }
        return null
    }

    composeView.layout(0, 0, width, height)

    val bitmap = createBitmap(width, height)
    bitmap.applyCanvas { composeView.draw(this) }

    containerView.removeView(composeView)

    return bitmap
}

/**
 * Prepares [view] for a rendering by attaching it as a descendant of this [MapView].
 * This is a trick to enable [ComposeView] to start its composition, as it requires being attached
 * to a window. A [ComposeUiViewRenderer.RenderHandle] is returned, which must be disposed after
 * this view no longer needs to render. Disposing removes [view] from the [MapView].
 */
internal fun MapView.startRenderingComposeView(
    view: AbstractComposeView,
    parentContext: CompositionContext,
): ComposeUiViewRenderer.RenderHandle {
    val containerView = ensureContainerView()
    containerView.addView(view)
    view.apply {
        setParentCompositionContext(parentContext)
    }
    return object : ComposeUiViewRenderer.RenderHandle {
        override fun dispose() {
            containerView.removeView(view)
        }

    }
}

/**
 * Retrieves the [NoDrawContainerView] from this [MapView], or adds one if there isn't already one.
 * @see NoDrawContainerView
 */
private fun MapView.ensureContainerView(): NoDrawContainerView {
    return findViewById(R.id.maps_compose_nodraw_container_view)
        ?: NoDrawContainerView(context)
            .apply { id = R.id.maps_compose_nodraw_container_view }
            .also(::addView)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun rememberComposeUiViewRenderer(): ComposeUiViewRenderer {
    val mapView = (currentComposer.applier as MapApplier).mapView
    val compositionContext = rememberCompositionContext()

    return remember(compositionContext) {
        object : ComposeUiViewRenderer {

            override fun renderViewOnce(
                view: AbstractComposeView,
                onAddedToWindow: (() -> Unit)?
            ) {
                mapView.renderComposeViewOnce(
                    view = view,
                    onAddedToWindow = onAddedToWindow?.let { { it() } },
                    parentContext = compositionContext,
                )
            }

            override fun startRenderingView(
                view: AbstractComposeView
            ): ComposeUiViewRenderer.RenderHandle {
                return mapView.startRenderingComposeView(
                    view = view,
                    parentContext = compositionContext,
                )
            }

        }
    }
}

/** @see MapView.renderComposeViewOnce */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface ComposeUiViewRenderer {

    /**
     * Prepares [view] for a single render by temporarily attaching it as a child of the [MapView].
     * Its composition will start. [onAddedToWindow] is called in place, and then [view] is removed
     * from the window before returning.
     */
    public fun renderViewOnce(
        view: AbstractComposeView,
        onAddedToWindow: (() -> Unit)?
    )

    public fun startRenderingView(
        view: AbstractComposeView
    ): RenderHandle

    public interface RenderHandle : Closeable {
        public fun dispose()

        override fun close(): Unit = dispose()
    }

}

/**
 * A ViewGroup that prevents its children from being laid out or drawn.
 * Used for adding ComposeViews as descendants of a MapView without actually affecting the view
 * hierarchy from the user's perspective.
 */
private class NoDrawContainerView(context: Context) : ViewGroup(context) {

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    }

    override fun dispatchDraw(canvas: Canvas) {
    }

}
