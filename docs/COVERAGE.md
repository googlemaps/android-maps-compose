# 📊 Maps Compose API Coverage Matrix

This document provides a comprehensive evaluation of the public Composable API surface of the Google Maps Platform Compose library on Android. It ensures all critical interfaces are accounted for and maps them directly to our snippet catalog.

---

## 📑 Core Library Coverage (`maps-compose`)

| Composable / API | Status | Snippet File & Tag Reference | Verification Status |
| :--- | :---: | :--- | :---: |
| **`GoogleMap`** | ✅ **100%** | [MapInitSnippets.kt](../snippets/src/main/java/com/google/maps/android/compose/snippets/MapInitSnippets.kt#L37)<br>Tag: `maps_android_compose_init_basic` | ✅ Verified |
| **`Marker`** | ✅ **100%** | [MarkerSnippets.kt](../snippets/src/main/java/com/google/maps/android/compose/snippets/MarkerSnippets.kt#L49)<br>Tag: `maps_android_compose_marker_basic` | ✅ Verified |
| **`MarkerComposable`** | ✅ **100%** | [MarkerSnippets.kt](../snippets/src/main/java/com/google/maps/android/compose/snippets/MarkerSnippets.kt#L106)<br>Tag: `maps_android_compose_marker_composable` | ✅ Verified |
| **`MarkerInfoWindowComposable`** | ✅ **100%** | [MarkerSnippets.kt](../snippets/src/main/java/com/google/maps/android/compose/snippets/MarkerSnippets.kt#L149)<br>Tag: `maps_android_compose_marker_info_window` | ✅ Verified |
| **`Polyline`** | ✅ **100%** | [ShapeSnippets.kt](../snippets/src/main/java/com/google/maps/android/compose/snippets/ShapeSnippets.kt#L38)<br>Tag: `maps_android_compose_polyline` | ✅ Verified |
| **`Polygon`** | ✅ **100%** | [ShapeSnippets.kt](../snippets/src/main/java/com/google/maps/android/compose/snippets/ShapeSnippets.kt#L71)<br>Tag: `maps_android_compose_polygon` | ✅ Verified |
| **`Circle`** | ✅ **100%** | [ShapeSnippets.kt](../snippets/src/main/java/com/google/maps/android/compose/snippets/ShapeSnippets.kt#L106)<br>Tag: `maps_android_compose_circle` | ✅ Verified |
| **`MapEffect`** | ✅ **100%** | [DataLayerSnippets.kt](../snippets/src/main/java/com/google/maps/android/compose/snippets/DataLayerSnippets.kt#L41)<br>Tag: `maps_android_compose_geojson_layer` | ✅ Verified |
| **`GroundOverlay`** | ✅ **100%** | [AdvancedSnippets.kt](../snippets/src/main/java/com/google/maps/android/compose/snippets/AdvancedSnippets.kt#L57)<br>Tag: `maps_android_compose_ground_overlay` | ✅ Verified |
| **`TileOverlay`** | ✅ **100%** | [AdvancedSnippets.kt](../snippets/src/main/java/com/google/maps/android/compose/snippets/AdvancedSnippets.kt#L90)<br>Tag: `maps_android_compose_tile_overlay` | ✅ Verified |
| **`rememberComposeBitmapDescriptor`**| ✅ **100%** | [AdvancedSnippets.kt](../snippets/src/main/java/com/google/maps/android/compose/snippets/AdvancedSnippets.kt#L155)<br>Tag: `maps_android_compose_remember_bitmap_descriptor` | ✅ Verified |

---

## 📑 Utility & Widget Coverage

| Composable / API | Status | Snippet File & Tag Reference | Verification Status |
| :--- | :---: | :--- | :---: |
| **`Clustering`** (utils) | ✅ **100%** | [ClusteringSnippets.kt](../snippets/src/main/java/com/google/maps/android/compose/snippets/ClusteringSnippets.kt#L57)<br>Tag: `maps_android_compose_clustering` | ✅ Verified |
| **`WmsTileOverlay`** (utils) | ✅ **100%** | [AdvancedSnippets.kt](../snippets/src/main/java/com/google/maps/android/compose/snippets/AdvancedSnippets.kt#L123)<br>Tag: `maps_android_compose_wms_tile_overlay` | ✅ Verified |
| **`ScaleBar`** (widgets) | ✅ **100%** | [AdvancedSnippets.kt](../snippets/src/main/java/com/google/maps/android/compose/snippets/AdvancedSnippets.kt#L194)<br>Tag: `maps_android_compose_scale_bar` | ✅ Verified |

---

## 📑 Standard KML & GeoJSON Integration
*(Loaded natively using `MapEffect` to safely obtain the underlying `GoogleMap` instance)*

| Integration Feature | Status | Snippet File & Tag Reference | Verification Status |
| :--- | :---: | :--- | :---: |
| **`GeoJsonLayer`** | ✅ **100%** | [DataLayerSnippets.kt](../snippets/src/main/java/com/google/maps/android/compose/snippets/DataLayerSnippets.kt#L41)<br>Tag: `maps_android_compose_geojson_layer` | ✅ Verified |
| **`KmlLayer`** | ✅ **100%** | [DataLayerSnippets.kt](../snippets/src/main/java/com/google/maps/android/compose/snippets/DataLayerSnippets.kt#L83)<br>Tag: `maps_android_compose_kml_layer` | ✅ Verified |
