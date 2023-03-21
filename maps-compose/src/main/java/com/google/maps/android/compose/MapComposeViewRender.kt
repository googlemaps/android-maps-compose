package com.google.maps.android.compose

import android.content.Context
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
