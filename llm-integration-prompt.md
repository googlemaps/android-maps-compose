# Android Maps Compose - AI Integration Prompt

You are an expert Android developer specializing in Jetpack Compose and modern Android architecture. Your task is to integrate the `android-maps-compose` library into the user's Android application.

Please follow these instructions carefully to ensure a complete and idiomatic implementation.

## 1. Setup Dependencies

You can add dependencies using either version catalogs (recommended) or directly in your `build.gradle.kts` file. Verify the latest versions if possible, but use these as a baseline:

### Option A: Using Version Catalogs (Recommended)

Add the following to your `gradle/libs.versions.toml` file:

```toml
[versions]
mapsCompose = "8.2.0" # x-release-please-version

[libraries]
maps-compose = { group = "com.google.maps.android", name = "maps-compose", version.ref = "mapsCompose" }
maps-compose-utils = { group = "com.google.maps.android", name = "maps-compose-utils", version.ref = "mapsCompose" }
maps-compose-widgets = { group = "com.google.maps.android", name = "maps-compose-widgets", version.ref = "mapsCompose" }
```

Then add them to your app-level `build.gradle.kts`:

```kotlin
dependencies {
    // Google Maps Compose library
    implementation(libs.maps.compose)

    // Optional: Maps Compose Utilities (for clustering, etc.)
    // implementation(libs.maps.compose.utils)

    // Optional: Maps Compose Widgets (for UI components)
    // implementation(libs.maps.compose.widgets)
}
```

### Option B: Direct Dependencies

If you are not using version catalogs, add the dependencies directly to your app-level `build.gradle.kts`:

```kotlin
dependencies {
    // Google Maps Compose library
    implementation("com.google.maps.android:maps-compose:8.2.0") // x-release-please-version

    // Optional: Maps Compose Utilities (for clustering, etc.)
    // implementation("com.google.maps.android:maps-compose-utils:8.2.0") // x-release-please-version

    // Optional: Maps Compose Widgets (for UI components)
    // implementation("com.google.maps.android:maps-compose-widgets:8.2.0") // x-release-please-version
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
*   **Lifecycle:** `GoogleMap` handles its own lifecycle under the hood in Compose, so you generally don't need to manually manage `MapView` lifecycle events unless doing custom integrations.

## 5. Execution Steps
1. Create a new branch `feature/maps-compose-integration`.
2. Add the Maps Compose dependencies to the app-level `build.gradle.kts`.
3. Set up the Secrets Gradle Plugin in both project-level and app-level `build.gradle.kts`.
4. Update `AndroidManifest.xml` with permissions and the `${MAPS_API_KEY}` placeholder.
5. Create the `MapScreen.kt` composable.
6. Provide a summary of the changes and instruct the user on how to add their API key to `local.properties`.