package com.google.maps.android.compose

import android.view.View
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.ui.UiComposable
import androidx.compose.ui.platform.ComposeView
import com.google.android.gms.maps.MapView

/**
 * Prepares [view] for a single render by temporarily attaching it as a child of this [MapView].
 * This is a trick to enable [ComposeView] to start its composition, as it requires being attached
 * to a window. [onAddedToWindow] is called in place, and then [view] is removed from the window
 * before returning.
 */
internal fun MapView.renderComposeView(
    view: ComposeView,
    onAddedToWindow: ((View) -> Unit)? = null,
    parentContext: CompositionContext,
) {
    addView(view)
    view.apply {
        setParentCompositionContext(parentContext)
    }
    onAddedToWindow?.invoke(view)
    removeView(view)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun rememberComposeUiViewRenderer(): ComposeUiViewRenderer {
    val mapView = (currentComposer.applier as MapApplier).mapView
    val compositionContext = rememberCompositionContext()

    return remember(compositionContext) {
        object : ComposeUiViewRenderer {

            override fun renderView(
                onAddedToWindow: ((View) -> Unit)?,
                content: @[UiComposable Composable] () -> Unit,
            ): ComposeView {
                val view = ComposeView(mapView.context)
                mapView.renderComposeView(
                    view = view.apply {
                        setContent(content)
                    },
                    onAddedToWindow = onAddedToWindow,
                    parentContext = compositionContext,
                )
                return view
            }

            override fun renderView(
                view: ComposeView,
                onAddedToWindow: (() -> Unit)?
            ) {
                mapView.renderComposeView(
                    view = view,
                    onAddedToWindow = onAddedToWindow?.let { { it() } },
                    parentContext = compositionContext,
                )
            }

        }
    }
}

/** @see MapView.renderComposeView */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface ComposeUiViewRenderer {

    /**
     * Creates a [ComposeView] and prepares it for a single render by temporarily attaching it as a
     * child of the [MapView].
     * [content] is composed, [onAddedToWindow] is called in place, and then the view is removed
     * from the window before returning.
     */
    public fun renderView(
        onAddedToWindow: ((View) -> Unit)?,
        content: @[UiComposable Composable] () -> Unit
    ): ComposeView

    /**
     * Prepares [view] for a single render by temporarily attaching it as a child of the [MapView].
     * Its composition will start. [onAddedToWindow] is called in place, and then [view] is removed
     * from the window before returning.
     */
    public fun renderView(
        view: ComposeView,
        onAddedToWindow: (() -> Unit)?
    )

}
