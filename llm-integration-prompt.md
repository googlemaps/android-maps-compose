# Android Maps Compose - AI Integration Prompt

You are an expert Android developer specializing in Jetpack Compose and modern Android architecture. Your task is to integrate the `android-maps-compose` library into the user's Android application.

Please follow these instructions carefully to ensure a complete and idiomatic implementation.

## 1. Setup Dependencies

First, add the necessary dependencies to the app-level `build.gradle.kts` file. 
Verify the latest versions if possible, but use these as a baseline:

```kotlin
dependencies {
    // Google Maps Compose library
    implementation("com.google.maps.android:maps-compose:8.2.0")
    
    // Google Maps Play Services
    implementation("com.google.android.gms:play-services-maps:8.2.0")

    // Optional: Maps Compose Utilities (for clustering, etc.)
    // implementation("com.google.maps.android:maps-compose-utils:8.2.0")
    
    // Optional: Maps Compose Widgets (for UI components)
    // implementation("com.google.maps.android:maps-compose-widgets:8.2.0")
}
```

## 2. Update AndroidManifest.xml

Add the required permissions and the Google Maps API key meta-data to `AndroidManifest.xml`. Instruct the user to replace `YOUR_API_KEY` with their actual API key from the Google Cloud Console.

```xml
<manifest ...>
    <!-- Required for Google Maps -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application ...>
        <!-- Google Maps API Key -->
        <meta-data
            android:name="com.google.android.geo.API_KEY"
            android:value="YOUR_API_KEY" />
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
2. Add the dependencies to `build.gradle.kts`.
3. Update `AndroidManifest.xml` with permissions and the API key placeholder.
4. Create the `MapScreen.kt` composable.
5. Provide a summary of the changes and instruct the user on how to add their API key.
