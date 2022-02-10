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
GoogleMap(
    modifier = Modifier.fillMaxSize(),
    googleMapOptionsFactory = {
        GoogleMapOptions().camera(CameraPosition.fromLatLngZoom(singapore, 10f))
    }
)
```

### Creating and configuring a map

Configuring the map can be done either by passing a `GoogleMapOptions` instance 
to initialize the map, or by passing a `MapProperties` object into the `GoogleMap`
composable.

```kotlin
// Initialize map by providing a googleMapOptionsFactory
GoogleMap(
    googleMapOptionsFactory = {
        GoogleMapOptions().mapId("MyMapId")
    }
)

// ...or set properties using MapProperties which you can use to recompose the map
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
```

### Controlling the map's camera

Camera changes and updates can be observed and controlled via `CameraPositionState`.

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

## Sample App

This repository includes a [sample app](app).

To run it, you'll have to:
1. Get a [Maps API key][api-key]
1. Add an entry in `local.properties` that looks like `MAPS_API_KEY=YOUR_KEY`
1. Build and run

## Installation

```groovy
dependencies {
    implementation 'com.google.maps.android:maps-compose:1.0.1'
    
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
