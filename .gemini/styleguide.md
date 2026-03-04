# Gemini Code Assist Style Guide: android-maps-compose

This guide defines the custom code review and generation rules for the `android-maps-compose` project.

## Jetpack Compose Guidelines
- **API Guidelines**: Strictly follow the [Jetpack Compose API guidelines](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md).
- **Naming**: Composable functions must be PascalCase.
- **State Management**: Prefer library-provided state holders like `rememberCameraPositionState` or `MarkerState`.
- **Modifiers**: The first optional parameter of any Composable should be `modifier: Modifier = Modifier`.

## Kotlin Style
- **Naming**: Use camelCase for variables and functions.
- **Documentation**: Provide KDoc for all public classes, properties, and functions.
- **Safety**: Use null-safe operators and avoid `!!`.

## Project Specifics
- **Secrets**: Never commit API keys. Ensure they are read from `secrets.properties` via `BuildConfig` or similar.
- **Maps SDK**: Use the components provided in `maps-compose`, `maps-compose-utils`, and `maps-compose-widgets` rather than raw `GoogleMap` objects.
