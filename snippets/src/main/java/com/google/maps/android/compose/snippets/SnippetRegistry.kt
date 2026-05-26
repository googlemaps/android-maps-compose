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

package com.google.maps.android.compose.snippets

import androidx.compose.runtime.Composable

data class SnippetItemInfo(
    val title: String,
    val description: String,
    val content: @Composable () -> Unit
)

data class SnippetGroupInfo(
    val title: String,
    val description: String,
    val items: List<SnippetItemInfo>
)

object SnippetRegistry {
    val groups: List<SnippetGroupInfo> by lazy {
        listOf(
            SnippetGroupInfo(
                title = "Map Initialization",
                description = "Snippets demonstrating map initialization and configuration.",
                items = listOf(
                    SnippetItemInfo(
                        title = "1. Basic Map",
                        description = "Initializes a simple Google Map.",
                        content = { BasicMapSnippet() }
                    ),
                    SnippetItemInfo(
                        title = "2. Custom Configuration",
                        description = "Initializes a Google Map with custom properties and UI settings.",
                        content = { CustomConfigMapSnippet() }
                    )
                )
            ),
            SnippetGroupInfo(
                title = "Camera Control",
                description = "Snippets demonstrating camera movement, animation, and boundaries.",
                items = listOf(
                    SnippetItemInfo(
                        title = "1. Move Camera",
                        description = "Instantly updates camera position.",
                        content = { MoveCameraSnippet() }
                    ),
                    SnippetItemInfo(
                        title = "2. Animate Camera",
                        description = "Smoothly animates camera position.",
                        content = { AnimateCameraSnippet() }
                    ),
                    SnippetItemInfo(
                        title = "3. Restrict Camera Bounds",
                        description = "Restricts camera movement to a specific LatLng bounds.",
                        content = { RestrictCameraBoundsSnippet() }
                    )
                )
            ),
            SnippetGroupInfo(
                title = "Markers",
                description = "Snippets demonstrating standard, custom, and Compose-rendered markers.",
                items = listOf(
                    SnippetItemInfo(
                        title = "1. Basic Marker",
                        description = "Adds a basic marker to Singapore.",
                        content = { BasicMarkerSnippet() }
                    ),
                    SnippetItemInfo(
                        title = "2. Custom Marker Icon",
                        description = "Adds a marker with a custom resource drawable icon.",
                        content = { CustomMarkerIconSnippet() }
                    ),
                    SnippetItemInfo(
                        title = "3. Marker Composable",
                        description = "Adds an interactive custom marker rendered using fully Jetpack Compose UI layouts.",
                        content = { MarkerComposableSnippet() }
                    ),
                    SnippetItemInfo(
                        title = "4. Custom Info Window Composable",
                        description = "Adds a custom InfoWindow rendered dynamically using fully Jetpack Compose UI.",
                        content = { CustomInfoWindowSnippet() }
                    )
                )
            ),
            SnippetGroupInfo(
                title = "Shapes",
                description = "Snippets demonstrating drawing Polylines, Polygons, and Circles on the map.",
                items = listOf(
                    SnippetItemInfo(
                        title = "1. Polyline",
                        description = "Draws a solid Polyline.",
                        content = { PolylineSnippet() }
                    ),
                    SnippetItemInfo(
                        title = "2. Polygon",
                        description = "Draws a filled Polygon.",
                        content = { PolygonSnippet() }
                    ),
                    SnippetItemInfo(
                        title = "3. Circle",
                        description = "Draws a solid filled Circle.",
                        content = { CircleSnippet() }
                    )
                )
            ),
            SnippetGroupInfo(
                title = "Clustering & Utilities",
                description = "Snippets demonstrating marker clustering and utility helpers.",
                items = listOf(
                    SnippetItemInfo(
                        title = "1. Marker Clustering",
                        description = "Clusters multiple nearby markers dynamically.",
                        content = { MarkerClusteringSnippet() }
                    )
                )
            ),
            SnippetGroupInfo(
                title = "Data Layers",
                description = "Snippets demonstrating importing and rendering GeoJSON and KML data layers.",
                items = listOf(
                    SnippetItemInfo(
                        title = "1. GeoJSON Layer",
                        description = "Loads a GeoJSON data layer on the map.",
                        content = { GeoJsonLayerSnippet() }
                    ),
                    SnippetItemInfo(
                        title = "2. KML Layer",
                        description = "Loads a KML data layer on the map.",
                        content = { KmlLayerSnippet() }
                    )
                )
            ),
            SnippetGroupInfo(
                title = "Overlays & Widgets",
                description = "Snippets demonstrating image overlays, custom tile layers, and UI widgets.",
                items = listOf(
                    SnippetItemInfo(
                        title = "1. Ground Overlay",
                        description = "Displays a static image clamped over coordinate bounds.",
                        content = { GroundOverlaySnippet() }
                    ),
                    SnippetItemInfo(
                        title = "2. Tile Overlay",
                        description = "Displays custom styled dynamic map tile overlays.",
                        content = { TileOverlaySnippet() }
                    ),
                    SnippetItemInfo(
                        title = "3. WMS Tile Overlay",
                        description = "Displays tiles from a Web Map Service dynamically.",
                        content = { WmsTileOverlaySnippet() }
                    ),
                    SnippetItemInfo(
                        title = "4. Compose Bitmap Descriptor",
                        description = "Converts a Compose Composable dynamically into a marker icon.",
                        content = { RememberComposeBitmapDescriptorSnippet() }
                    ),
                    SnippetItemInfo(
                        title = "5. Scale Bar Widget",
                        description = "Overlay showing on-screen distance ratio scales based on zoom.",
                        content = { ScaleBarSnippet() }
                    )
                )
            )
        )
    }
}
