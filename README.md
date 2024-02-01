![Tests](https://github.com/googlemaps/android-maps-compose/actions/workflows/test.yml/badge.svg)
![Stable](https://img.shields.io/badge/stability-stable-green)
[![Discord](https://img.shields.io/discord/676948200904589322)][Discord server]
![Apache-2.0](https://img.shields.io/badge/license-Apache-blue)

# Maps Compose 🗺

## Description

This repository contains [Jetpack Compose][jetpack-compose] components for the [Maps SDK for Android][maps-sdk].

## Requirements

* Kotlin-enabled project
* Jetpack Compose-enabled project (see [releases](https://github.com/googlemaps/android-maps-compose/releases) for the required version of Jetpack Compose)
* An [API key][api-key]
* API level 21+

## Installation

You no longer need to specify the Maps SDK for Android or its Utility Library as separate dependencies, since `maps-compose` and `maps-compose-utils` pull in the appropriate versions of these respectively.

```groovy
dependencies {
    implementation 'com.google.maps.android:maps-compose:4.3.3'

    // Optionally, you can include the Compose utils library for Clustering,
    // Street View metadata checks, etc.
    implementation 'com.google.maps.android:maps-compose-utils:4.3.3'

    // Optionally, you can include the widgets library for ScaleBar, etc.
    implementation 'com.google.maps.android:maps-compose-widgets:4.3.3'
}
```

## Sample App

This repository includes a [sample app](app).

To run it:

1. Get a [Maps API key][api-key]
1. Create a file in the root directory named `local.properties` with a single line that looks like this, replacing YOUR_KEY with the key from step 1: `MAPS_API_KEY=YOUR_KEY`
1. Build and run

## Documentation

You can learn more about all the extensions provided by this library by reading the [reference documents][Javadoc].

## Usage

Adding a map to your app looks like the following:

```kotlin
val singapore = LatLng(1.35, 103.87)
val cameraPositionState = rememberCameraPositionState {
    position = CameraPosition.fromLatLngZoom(singapore, 10f)
}
GoogleMap(
    modifier = Modifier.fillMaxSize(),
    cameraPositionState = cameraPositionState
)
```

<details>
  <summary>Creating and configuring a map</summary>

## Creating and configuring a map

Configuring the map can be done by passing a `MapProperties` object into the
`GoogleMap` composable, or for UI-related configurations, use `MapUiSettings`.
`MapProperties` and `MapUiSettings` should be your first go-to for configuring
the map. For any other configuration not present in those two classes, use
`googleMapOptionsFactory` to provide a `GoogleMapOptions` instance instead.
Typically, anything that can only be provided once (i.e. when the map is
created)—like map ID—should be provided via `googleMapOptionsFactory`.

```kotlin
// Set properties using MapProperties which you can use to recompose the map
var mapProperties by remember {
    mutableStateOf(
        MapProperties(maxZoomPreference = 10f, minZoomPreference = 5f)
    )
}
var mapUiSettings by remember {
    mutableStateOf(
        MapUiSettings(mapToolbarEnabled = false)
    )
}
Box(Modifier.fillMaxSize()) {
    GoogleMap(properties = mapProperties, uiSettings = mapUiSettings)
    Column {
        Button(onClick = {
            mapProperties = mapProperties.copy(
                isBuildingEnabled = !mapProperties.isBuildingEnabled
            )
        }) {
            Text(text = "Toggle isBuildingEnabled")
        }
        Button(onClick = {
            mapUiSettings = mapUiSettings.copy(
                mapToolbarEnabled = !mapUiSettings.mapToolbarEnabled
            )
        }) {
            Text(text = "Toggle mapToolbarEnabled")
        }
    }
}

// ...or initialize the map by providing a googleMapOptionsFactory
// This should only be used for values that do not recompose the map such as
// map ID.
GoogleMap(
    googleMapOptionsFactory = {
        GoogleMapOptions().mapId("MyMapId")
    }
)

```

</details>

<details>
  <summary>Controlling a map's camera</summary>

### Controlling a map's camera

Camera changes and updates can be observed and controlled via `CameraPositionState`.

**Note**: `CameraPositionState` is the source of truth for anything camera
related. So, providing a camera position in `GoogleMapOptions` will be
overridden by `CameraPosition`.

```kotlin
val singapore = LatLng(1.35, 103.87)
val cameraPositionState: CameraPositionState = rememberCameraPositionState {
    position = CameraPosition.fromLatLngZoom(singapore, 11f)
}
Box(Modifier.fillMaxSize()) {
  GoogleMap(cameraPositionState = cameraPositionState)
  Button(onClick = {
    // Move the camera to a new zoom level
    cameraPositionState.move(CameraUpdateFactory.zoomIn())
  }) {
      Text(text = "Zoom In")
  }
}
```

</details>

<details>
  <summary>Drawing on a map</summary>

### Drawing on a map

Drawing on the map, such as adding markers, can be accomplished by adding child
composable elements to the content of the `GoogleMap`.

```kotlin
GoogleMap(
    googleMapOptionsFactory = {
        GoogleMapOptions().mapId("DEMO_MAP_ID")
    },
    //...
) {
    AdvancedMarker(
        state = MarkerState(position = LatLng(-34, 151)),
        title = "Marker in Sydney"
    )
    AdvancedMarker(
        state = MarkerState(position = LatLng(35.66, 139.6)),
        title = "Marker in Tokyo"
    )
}
```

You can customize a marker by using `PinConfig` with an `AdvancedMarker`.

```kotlin
val state = MyState()

GoogleMap(
    googleMapOptionsFactory = {
        GoogleMapOptions().mapId("DEMO_MAP_ID")
    },
    //...
) {
    val pinConfig = PinConfig.builder()
        .setBackgroundColor(Color.MAGENTA)
        .build()

    AdvancedMarker(
        state = MarkerState(position = LatLng(-34, 151)),
        title = "Magenta marker in Sydney",
        pinConfig = pinConfig
    )
}
```

</details>

<details>
  <summary>Recomposing elements</summary>

### Recomposing elements

Markers and other elements need to be recomposed in the screen. To achieve recomposition, you can set mutable properties of state objects:

```kotlin
val markerState = rememberMarkerState(position = singapore)

//...

LaunchedEffect(Unit) {
    repeat(10) {
        delay(5.seconds)
        val old = markerState.position
        markerState.position = LatLng(old.latitude + 1.0, old.longitude + 2.0)
    }
}
```

In the example above, recomposition occurs as `MarkerState.position` is updated with different values over time, shifting the Marker around the screen.


</details>
<details>
  <summary>Customizing a marker's info window</summary>

### Customizing a marker's info window

You can customize a marker's info window contents by using the
`MarkerInfoWindowContent` element, or if you want to customize the entire info
window, use the `MarkerInfoWindow` element instead. Both of these elements
accept a `content` parameter to provide your customization in a composable
lambda expression.

```kotlin
MarkerInfoWindowContent(
    //...
) { marker ->
    Text(marker.title ?: "Default Marker Title", color = Color.Red)
}

MarkerInfoWindow(
    //...
) { marker ->
    // Implement the custom info window here
    Column {
        Text(marker.title ?: "Default Marker Title", color = Color.Red)
        Text(marker.snippet ?: "Default Marker Snippet", color = Color.Red)
    }
}
```

</details>

<details>
  <summary>Street View</summary>

### Street View

You can add a Street View given a location using the `StreetView` composable.

1. Test whether a Street View location is valid with the the
`fetchStreetViewData` utility from the [`maps-compose-utils` library](#maps-compose-utility-library).

```kotlin
 streetViewResult =
    fetchStreetViewData(singapore, BuildConfig.MAPS_API_KEY)
```

2. Once the location is confirmed valid, add a Street View composable by providing a `StreetViewPanoramaOptions` object.

```kotlin
val singapore = LatLng(1.3588227, 103.8742114)
StreetView(
    streetViewPanoramaOptionsFactory = {
        StreetViewPanoramaOptions().position(singapore)
    }
)
```

</details>

<details>
  <summary>Controlling the map directly (experimental)</summary>

## Controlling the map directly (experimental)

Certain use cases may require extending the `GoogleMap` object to decorate / augment
the map. It can be obtained with the `MapEffect` Composable.
Doing so can be dangerous, as the `GoogleMap` object is managed by this library.

```kotlin
GoogleMap(
    // ...
) {
    MapEffect { map ->
        // map is the GoogleMap
    }
}
```

</details>

## Maps Compose Utility Library

This library provides optional utilities in the `maps-compose-utils` library from the [Maps SDK for Android Utility Library](https://github.com/googlemaps/android-maps-utils).

### Clustering

The marker clustering utility helps you manage multiple markers at different zoom levels.
When a user views the map at a high zoom level, the individual markers show on the map. When the user zooms out, the markers gather together into clusters, to make viewing the map easier.

The [MarkerClusteringActivity](app/src/main/java/com/google/maps/android/compose/MarkerClusteringActivity.kt) demonstrates usage.

```kotlin
Clustering(
    items = items,
    // Optional: Handle clicks on clusters, cluster items, and cluster item info windows
    onClusterClick = null,
    onClusterItemClick = null,
    onClusterItemInfoWindowClick = null,
    // Optional: Custom rendering for clusters
    clusterContent = null,
    // Optional: Custom rendering for non-clustered items
    clusterItemContent = null,
)
```

### Street View metadata utility

The `fetchStreetViewData` method provides functionality to check whether a location is supported in StreetView. You can avoid errors when adding a Street View panorama to an Android app by calling this metadata utility and only adding a Street View panorama if the response is OK.

> [!IMPORTANT]
> Be sure to [enable Street View Static API](https://goo.gle/enable-sv-static-api) on the project associated with your API key.

You can see example usage
in the [`StreetViewActivity`](https://github.com/googlemaps/android-maps-compose/blob/main/app/src/main/java/com/google/maps/android/compose/StreetViewActivity.kt) of the demo app:

```kotlin
 streetViewResult =
    fetchStreetViewData(singapore, BuildConfig.MAPS_API_KEY)
```

## Maps Compose Widgets

This library also provides optional composable widgets in the `maps-compose-widgets` library that you can use alongside the `GoogleMap` composable.

### ScaleBar

This widget shows the current scale of the map in feet and meters when zoomed into the map, changing to miles and kilometers, respectively, when zooming out. A `DisappearingScaleBar` is also included, which appears when the zoom level of the map changes, and then disappears after a configurable timeout period.

The [ScaleBarActivity](app/src/main/java/com/google/maps/android/compose/ScaleBarActivity.kt) demonstrates both of these, with the `DisappearingScaleBar` in the upper left corner and the normal base `ScaleBar` in the upper right:

![maps-compose-scale-bar-cropped](https://user-images.githubusercontent.com/928045/175665891-a0635004-2201-4392-83b3-0c6553b96926.gif)

Both versions of this widget leverage the `CameraPositionState` in `maps-compose` and therefore are very simple to configure with their defaults:

```kotlin
Box(Modifier.fillMaxSize()) {

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        // ... your map composables ...
    }

    ScaleBar(
        modifier = Modifier
            .padding(top = 5.dp, end = 15.dp)
            .align(Alignment.TopEnd),
        cameraPositionState = cameraPositionState
    )

    // OR

    DisappearingScaleBar(
        modifier = Modifier
            .padding(top = 5.dp, end = 15.dp)
            .align(Alignment.TopStart),
        cameraPositionState = cameraPositionState
    )
}
```

The colors of the text, line, and shadow are also all configurable (e.g., based on `isSystemInDarkTheme()` on a dark map). Similarly, the `DisappearingScaleBar` animations can be configured.

## Contributing

Contributions are welcome and encouraged! See [contributing] for more info.

## Support

This library is offered via an open source [license](LICENSE). It is not governed by the Google Maps Platform [Technical Support Services Guidelines](https://cloud.google.com/maps-platform/terms/tssg?utm_source=github&utm_medium=documentation&utm_campaign=&utm_content=android_oss), the [SLA](https://cloud.google.com/maps-platform/terms/sla?utm_source=github&utm_medium=documentation&utm_campaign=&utm_content=android_oss), or the [Deprecation Policy](https://cloud.google.com/maps-platform/terms?utm_source=github&utm_medium=documentation&utm_campaign=&utm_content=android_oss) (however, any Google Maps Platform services used by the library remain subject to the Google Maps Platform Terms of Service).

This library adheres to [semantic versioning](https://semver.org/) to indicate when backwards-incompatible changes are introduced.

If you find a bug, or have a feature request, please [file an issue] on GitHub.

If you would like to get answers to technical questions from other Google Maps Platform developers, ask through one of our [developer community channels](https://developers.google.com/maps/developer-community?utm_source=github&utm_medium=documentation&utm_campaign=&utm_content=android_oss) including the Google Maps Platform [Discord server].

[maps-sdk]: https://developers.google.com/maps/documentation/android-sdk
[api-key]: https://developers.google.com/maps/documentation/android-sdk/get-api-key
[Discord server]: https://discord.gg/hYsWbmk
[Javadoc]: https://googlemaps.github.io/android-maps-compose
[contributing]: CONTRIBUTING.md
[code of conduct]: CODE_OF_CONDUCT.md
[file an issue]: https://github.com/googlemaps/android-maps-compose/issues/new/choose
[pull request]: https://github.com/googlemaps/android-maps-compose/compare
[jetpack-compose]: https://developer.android.com/jetpack/compose
