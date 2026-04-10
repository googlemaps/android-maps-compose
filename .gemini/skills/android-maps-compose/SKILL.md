---
name: android-maps-compose
description: Guide for integrating the android-maps-compose library into an Android application. Use when users ask to add Google Maps Compose to their Android app or set up Maps in Compose.
---

# Android Maps Compose Integration

You are an expert Android developer specializing in Jetpack Compose and modern Android architecture. Follow these instructions carefully to integrate the `android-maps-compose` library into the user's Android application.

## 1. Setup Dependencies

First, add the necessary dependencies to the app-level `build.gradle.kts` file.
Verify the latest versions if possible, but use these as a baseline:

```kotlin
dependencies {
    // Google Maps Compose library
    implementation("com.google.maps.android:maps-compose:8.3.0") // x-release-please-version

    // Optional: Maps Compose Utilities (for clustering, WMS tile overlays, etc.)
    implementation("com.google.maps.android:maps-compose-utils:8.3.0") // x-release-please-version

    // Optional: Maps Compose Widgets (for UI components)
    // implementation("com.google.maps.android:maps-compose-widgets:8.3.0") // x-release-please-version
}
```

## 2. Setup the Secrets Gradle Plugin

Instead of hardcoding the Google Maps API key in `AndroidManifest.xml`, use the Secrets Gradle Plugin for Android to inject the API key securely.

First, add the plugin to the project-level `build.gradle.kts`:

```kotlin
buildscript {
    dependencies {
        classpath("com.google.android.libraries.mapsplatform.secrets-gradle-plugin:secrets-gradle-plugin:2.0.1")
    }
}
```

Then, apply the plugin in the app-level `build.gradle.kts`:

```kotlin
plugins {
    // ...
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}
```

Add the API Key to `local.properties`:

```properties
MAPS_API_KEY=YOUR_API_KEY
```

In `AndroidManifest.xml`, add the required permissions and reference the injected API key meta-data:

```xml
<manifest ...>
    <!-- Required for Google Maps -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application ...>
        <!-- Google Maps API Key injected by Secrets Gradle Plugin -->
        <meta-data
            android:name="com.google.android.geo.API_KEY"
            android:value="${MAPS_API_KEY}" />
        ...
    </application>
</manifest>
```

## 3. Implement the Map Composable

Create a new file named `MapScreen.kt` (or similar, depending on the app's architecture) and add a basic Jetpack Compose map implementation.

Use `CameraPositionState` to control the camera and `Marker` to display points of interest.

```kotlin
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun MapScreen() {
    // Default location (e.g., Singapore)
    val defaultLocation = LatLng(1.35, 103.87)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 10f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        Marker(
            state = MarkerState(position = defaultLocation),
            title = "Singapore",
            snippet = "Marker in Singapore"
        )
    }
}
```

## 4. Best Practices & Guidelines
*   **State Management:** Hoist state (like camera position and marker lists) to the ViewModel if the map is dynamic.
*   **Performance:** For large numbers of markers, use the `Clustering` composable from the `maps-compose-utils` artifact instead of rendering thousands of individual `Marker` composables.
*   **Custom Markers (Experimental):** Use the experimental `rememberComposeBitmapDescriptor` function to create highly customizable marker icons directly from Compose content.
    ```kotlin
    import com.google.maps.android.compose.rememberComposeBitmapDescriptor
    import androidx.compose.material.Icon // Example for an icon
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.LocationOn

    // Inside your Composable where you want a custom marker
    val customMarkerIcon = rememberComposeBitmapDescriptor(key1 = "customMarker") {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Custom Location",
            // Add other modifiers or styling as needed
        )
    }
    // Then use it with your Marker:
    // Marker(state = MarkerState(position = defaultLocation), icon = customMarkerIcon, ...)
    ```
*   **WMS Tile Overlays:** To display Web Map Service (WMS) layers using EPSG:3857 projection, use the `WmsTileOverlay` composable from the `maps-compose-utils` artifact. This allows for dynamic and customizable tile sources.
    ```kotlin
    import com.google.maps.android.compose.wms.WmsTileOverlay
    import androidx.core.net.toUri

    // Inside your GoogleMap composable
    WmsTileOverlay(
        urlFormatter = { xMin, yMin, xMax, yMax, _ ->
            "https://basemap.nationalmap.gov/arcgis/services/USGSShadedReliefOnly/MapServer/WmsServer".toUri()
                .buildUpon()
                .appendQueryParameter("SERVICE", "WMS")
                .appendQueryParameter("VERSION", "1.1.1")
                .appendQueryParameter("REQUEST", "GetMap")
                .appendQueryParameter("FORMAT", "image/png")
                .appendQueryParameter("TRANSPARENT", "true")
                .appendQueryParameter("LAYERS", "0")
                .appendQueryParameter("SRS", "EPSG:3857")
                .appendQueryParameter("WIDTH", "256")
                .appendQueryParameter("HEIGHT", "256")
                .appendQueryParameter("STYLES", "")
                .appendQueryParameter("BBOX", "$xMin,$yMin,$xMax,$yMax")
                .build()
                .toString()
        },
        transparency = 0.5f
    )
    ```
*   **Lifecycle:** `GoogleMap` handles its own lifecycle under the hood in Compose, so you generally don't need to manually manage `MapView` lifecycle events unless doing custom integrations.
*   **Lite Mode Improvements:** Camera animations (`cameraPositionState.animate`) now gracefully fall back to instantaneous `moveCamera` operations when the map is in Lite Mode, preventing previous hangs.

## 5. Execution Steps
1. Create a new branch `feature/maps-compose-integration`.
2. Add the Maps Compose dependencies to the app-level `build.gradle.kts`, ensuring versions are `8.3.0`.
3. Set up the Secrets Gradle Plugin in both project-level and app-level `build.gradle.kts`.
4. Update `AndroidManifest.xml` with permissions and the `${MAPS_API_KEY}` placeholder.
5. Create the `MapScreen.kt` composable.
6. Provide a summary of the changes and instruct the user on how to add their API key to `local.properties`.
