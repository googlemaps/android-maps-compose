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

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.rememberCompositionContext
import com.google.android.gms.maps.model.Marker

/**
 * Supplies info windows for markers that no `Marker` composable owns, such as the markers that a
 * cluster manager adds to the map.
 *
 * When the map asks for the info window of such a marker, maps-compose asks each handler in turn.
 * The first non-null content renders as a child of its handler's composition, so it sees that
 * composition's locals. If a handler returns null, the next handler gets the marker. If no handler
 * has content for the marker, the map shows its default info window.
 *
 * @param infoWindow returns the content of the entire info window for a marker, or null if this
 * handler has none for it
 * @param infoContent returns the content shown inside the default info window frame for a marker,
 * or null if this handler has none for it. The map asks for this content only if it got no info
 * window for the marker.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun InfoWindowHandler(
    infoWindow: ((Marker) -> (@Composable () -> Unit)?)? = null,
    infoContent: ((Marker) -> (@Composable () -> Unit)?)? = null,
) {
    val compositionContext = rememberCompositionContext()
    ComposeNode<InfoWindowHandlerNode, MapApplier>(
        factory = { InfoWindowHandlerNode(compositionContext, infoWindow, infoContent) },
        update = {
            update(infoWindow) { this.infoWindow = it }
            update(infoContent) { this.infoContent = it }
        },
    )
}

internal class InfoWindowHandlerNode(
    val compositionContext: CompositionContext,
    var infoWindow: ((Marker) -> (@Composable () -> Unit)?)?,
    var infoContent: ((Marker) -> (@Composable () -> Unit)?)?,
) : MapNode
