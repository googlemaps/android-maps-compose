![Tests](https://github.com/googlemaps/android-maps-compose/actions/workflows/test.yml/badge.svg)
![Stable](https://img.shields.io/badge/stability-stable-green)
[![Discord](https://img.shields.io/discord/676948200904589322)][Discord channel]
![Apache-2.0](https://img.shields.io/badge/license-Apache-blue)

Maps Compose 🗺
===============

## Description
This repository contains [Jetpack Compose][jetpack-compose] components for the Maps SDK for Android.

## Requirements
* Kotlin-enabled project
* Jetpack Compose-enabled project
* An [API key][api-key]
* API level 21+

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

### Creating and configuring a map

Configuring the map can be done by passing a `MapProperties` object into the 
`GoogleMap` composable. For anything not available in `MapProperties` (typically 
anything that can only be provided once - like map ID), provide a 
`GoogleMapOptions` instance in the `googleMapOptionsFactory` properties instead.

```kotlin
// Set properties using MapProperties which you can use to recompose the map
var mapProperties by remember {
    mutableStateOf(
        MapProperties(maxZoomPreference = 10f, minZoomPreference = 5f)
    )
}
Box(Modifier.fillMaxSize()) {
    GoogleMap(properties = mapProperties)
    Button(onClick = {
        mapProperties = mapProperties.copy(
            isBuildingEnabled = !mapProperties.isBuildingEnabled
        )
    }) {
        Text(text = "Toggle isBuildingEnabled")
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

### Drawing on a map

Drawing on the map, such as adding markers, can be accomplished by adding child 
composable elements to the content of the `GoogleMap`.

```kotlin
GoogleMap(
  //...
) {
    Marker(position = LatLng(-34, 151), title = "Marker in Sydney")
    Marker(position = LatLng(35.66, 139.6), title = "Marker in Tokyo")
}
```

#### Customizing a marker's info window

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

## Sample App

This repository includes a [sample app](app).

To run it, you'll have to:
1. Get a [Maps API key][api-key]
1. Add an entry in `local.properties` that looks like `MAPS_API_KEY=YOUR_KEY`
1. Build and run

## Installation

```groovy
dependencies {
    implementation 'com.google.maps.android:maps-compose:1.2.0'
    
    // Make sure to also include the latest version of the Maps SDK for Android 
    implementation 'com.google.android.gms:play-services-maps:18.0.2'
}
```

## Documentation

You can learn more about all the extensions provided by this library by reading the [reference documents][Javadoc].

## Contributing

Contributions are welcome and encouraged! See [contributing] for more info.

## Support

Encounter an issue while using this library?

If you find a bug or have a feature request, please [file an issue].
Or, if you'd like to contribute, send us a [pull request] and refer to our [code of conduct].

You can also reach us on our [Discord channel].

[api-key]: https://developers.google.com/maps/documentation/android-sdk/get-api-key
[Discord channel]: https://discord.gg/hYsWbmk
[Javadoc]: https://googlemaps.github.io/android-maps-compose
[contributing]: CONTRIBUTING.md
[code of conduct]: CODE_OF_CONDUCT.md
[file an issue]: https://github.com/googlemaps/android-maps-compose/issues/new/choose
[pull request]: https://github.com/googlemaps/android-maps-compose/compare
[jetpack-compose]: https://developer.android.com/jetpack/compose
