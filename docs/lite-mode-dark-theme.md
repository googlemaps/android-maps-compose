# Supporting Dark Theme in Lite Mode Maps

This guide explains how to make a Lite Mode map in [Maps Compose](https://github.com/googlemaps/android-maps-compose) follow the Android system theme and switch to a dark palette automatically.

---

## Overview

In standard (interactive) mode, Maps Compose supports dark mode directly via the `mapColorScheme` parameter:

```kotlin
GoogleMap(
    mapColorScheme = ComposeMapColorScheme.FOLLOW_SYSTEM
)
```

However, when Lite Mode is enabled (`GoogleMapOptions().liteMode(true)`), the underlying Google Maps Android SDK (`play-services-maps`) functions differently:

1. **Static Pre-rendered Tiles**: Lite Mode maps fetch static raster tiles rather than rendering vectors on the client GPU.
2. **`MapColorScheme` Bypassed**: The Google Maps Android SDK explicitly ignores dynamic `MapColorScheme` configurations for Lite Mode maps.
3. **Map Types Remain in Light Mode**: Neither `MapType.NORMAL` nor `MapType.TERRAIN` will render dark tiles through `mapColorScheme`.

To support dark mode on a Lite Mode map, your application must listen for system theme changes and dynamically supply a custom dark JSON style using `MapProperties.mapStyleOptions`.

---

## Implementation Steps

### 1. Define a Dark Map Style JSON

Lite Mode maps support styling through standard Google Maps JSON style definitions. Define a compact dark palette constant (or store it in `res/raw/map_style_dark.json`):

```kotlin
private const val DARK_STYLE_JSON = """[
  {"elementType": "geometry", "stylers": [{"color": "#242f3e"}]},
  {"elementType": "labels.text.stroke", "stylers": [{"color": "#242f3e"}]},
  {"elementType": "labels.text.fill", "stylers": [{"color": "#746855"}]},
  {"featureType": "administrative.locality", "elementType": "labels.text.fill", "stylers": [{"color": "#d59563"}]},
  {"featureType": "poi", "elementType": "labels.text.fill", "stylers": [{"color": "#d59563"}]},
  {"featureType": "poi.park", "elementType": "geometry", "stylers": [{"color": "#263c3f"}]},
  {"featureType": "poi.park", "elementType": "labels.text.fill", "stylers": [{"color": "#6b9a76"}]},
  {"featureType": "road", "elementType": "geometry", "stylers": [{"color": "#38414e"}]},
  {"featureType": "road", "elementType": "geometry.stroke", "stylers": [{"color": "#212a37"}]},
  {"featureType": "road", "elementType": "labels.text.fill", "stylers": [{"color": "#9ca5b3"}]},
  {"featureType": "road.highway", "elementType": "geometry", "stylers": [{"color": "#746855"}]},
  {"featureType": "water", "elementType": "geometry", "stylers": [{"color": "#17263c"}]},
  {"featureType": "water", "elementType": "labels.text.fill", "stylers": [{"color": "#515c6d"}]}
]"""
```

### 2. Observe the System Setting in Compose

In Jetpack Compose, `isSystemInDarkTheme()` observes `LocalConfiguration.current` and automatically triggers recomposition whenever the user changes the system theme or when an automated schedule activates.

```kotlin
val isSystemDark = isSystemInDarkTheme()
```

### 3. Dynamically Apply `MapStyleOptions`

Construct a `MapProperties` instance that applies `MapStyleOptions` conditionally based on the active dark theme state:

```kotlin
val mapProperties = remember(isSystemDark) {
    MapProperties(
        mapType = MapType.NORMAL,
        mapStyleOptions = if (isSystemDark) MapStyleOptions(DARK_STYLE_JSON) else null
    )
}
```

### 4. Provide Lite Mode Options

Create the `GoogleMap` composable passing `GoogleMapOptions().liteMode(true)` via `googleMapOptionsFactory`:

```kotlin
GoogleMap(
    modifier = Modifier.fillMaxSize(),
    googleMapOptionsFactory = {
        GoogleMapOptions().liteMode(true)
    },
    properties = mapProperties,
)
```

---

## Full Working Example

Below is a complete, self-contained composable showing how to combine system theme observation with user overrides (`System`, `Light`, `Dark`):

```kotlin
enum class ThemeOption {
    SYSTEM,
    LIGHT,
    DARK
}

@Composable
fun ThemedLiteMapScreen() {
    var selectedTheme by rememberSaveable { mutableStateOf(ThemeOption.SYSTEM) }
    val systemInDarkTheme = isSystemInDarkTheme()

    val isDark = when (selectedTheme) {
        ThemeOption.SYSTEM -> systemInDarkTheme
        ThemeOption.LIGHT -> false
        ThemeOption.DARK -> true
    }

    val mapProperties = remember(isDark) {
        MapProperties(
            mapType = MapType.NORMAL,
            mapStyleOptions = if (isDark) MapStyleOptions(DARK_STYLE_JSON) else null
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Theme selector
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTheme == ThemeOption.SYSTEM,
                onClick = { selectedTheme = ThemeOption.SYSTEM },
                label = { Text("System") }
            )
            FilterChip(
                selected = selectedTheme == ThemeOption.LIGHT,
                onClick = { selectedTheme = ThemeOption.LIGHT },
                label = { Text("Light") }
            )
            FilterChip(
                selected = selectedTheme == ThemeOption.DARK,
                onClick = { selectedTheme = ThemeOption.DARK },
                label = { Text("Dark") }
            )
        }

        // Lite Mode Map
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                googleMapOptionsFactory = { GoogleMapOptions().liteMode(true) },
                properties = mapProperties,
            )
        }
    }
}
```

---

## Best Practices

* **Memoize Properties**: Wrap `MapProperties` creation in `remember(isDark, mapType)` to avoid allocating new style options on unrelated recompositions.
* **Preserve State Across Recreations**: Use `rememberSaveable` for any user-selected theme or map settings so selections persist when the device configuration changes.
* **Camera Movement**: In Lite Mode, `cameraPositionState.animate(...)` completes immediately without continuous panning animations.
* **Interactive Mode Alternative**: If native vector dark terrain shaders or full gesture interactions are needed, use interactive mode (`liteMode(false)`) with `mapColorScheme = ComposeMapColorScheme.FOLLOW_SYSTEM`.

---

## Sample Code

A complete, runnable demonstration of this pattern is available in the sample application:
* [LiteModeActivity.kt](../maps-app/src/main/java/com/google/maps/android/compose/LiteModeActivity.kt)
