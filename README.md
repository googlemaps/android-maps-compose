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
val sanFrancisco = LatLng(37.76, -122.47)
val cameraPositionState = rememberCameraPositionState(
    initialPosition = CameraPosition.fromLatLngZoom(sanFrancisco, 10f)
)
GoogleMap(
    modifier = Modifier.fillMaxSize(),
    cameraPositionState = cameraPositionState
)
```

### Creating and configuring a map

Configuring the map can be done either by passing a `MapOptions` instance to 
initialize the map (for properties that should only be set once), or by using 
`MapPropertiesState`—an object with stateful properties which trigger 
recomposition when changed.

```kotlin
val mapOptions = MapOptions(mapId = "MyMapId")
val mapPropertiesState = rememberMapPropertiesState()
GoogleMap(
    mapOptions = mapOptions,
    mapPropertiesState = mapPropertiesState,
)

// ...this triggers recomposition of the map
mapProperties.isIndoorEnabled = true
```

### Controlling the map's camera

Camera changes and updates can be observed and controlled via `CameraPositionState`.

```kotlin
val cameraPositionState: CameraPositionState = rememberCameraPositionState()
Box(Modifier.fillMaxSize()) {
  GoogleMap(cameraPositionState = cameraPositionState)
  Button(onClick = {
    // Move the camera to a new zoom level
    cameraPositionState.moveCamera(CameraUpdateFactory.zoomIn())
  })
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

## Sample App

This repository includes a [sample app](app).

To run it, you'll have to:
1. Get a [Maps API key][api-key]
1. Add an entry in `local.properties` that looks like `MAPS_API_KEY=YOUR_KEY`
1. Build and run

## Installation

```groovy
dependencies {
    implementation 'com.google.maps.android:maps-compose:1.0.0'
}
```

## Documentation

You can learn more about all the extensions provided by this library by reading the [reference documents][Javadoc].

## Support

Encounter an issue while using this library?

If you find a bug or have a feature request, please [file an issue].
Or, if you'd like to contribute, send us a [pull request] and refer to our [code of conduct].

You can also reach us on our [Discord channel].

[api-key]: https://developers.google.com/maps/documentation/android-sdk/get-api-key
[Discord channel]: https://discord.gg/hYsWbmk
[Javadoc]: https://googlemaps.github.io/android-maps-compose
[code of conduct]: CODE_OF_CONDUCT.md
[file an issue]: https://github.com/googlemaps/android-maps-compose/issues/new/choose
[pull request]: https://github.com/googlemaps/android-maps-compose/compare
[jetpack-compose]: https://developer.android.com/jetpack/compose
