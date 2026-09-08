# AGENTS.md

Guidance for AI coding agents working on this repository. For human contribution
rules (CLA, PR process, AI-assisted contribution policy), see [CONTRIBUTING.md](CONTRIBUTING.md).

## Project overview

Jetpack Compose components for the Maps SDK for Android. The published
libraries live in three modules; the rest of the repo supports them.

| Module | Purpose |
| --- | --- |
| `maps-compose` | Core library: `GoogleMap` composable, camera state, markers, shapes |
| `maps-compose-utils` | Utilities layer: clustering and other android-maps-utils integrations |
| `maps-compose-widgets` | Widget composables built on top of the core library |
| `maps-app` | Demo app exercising the libraries |
| `docs` | Dokka documentation aggregation |

Shared Gradle conventions are in `build-logic/` (included build).

## Building and testing

```bash
./gradlew assembleDebug                          # build everything
./gradlew :maps-compose:testDebugUnitTest        # unit tests for one module
./gradlew test                                   # all unit tests
./gradlew lint                                   # Android Lint
./gradlew jacocoAggregateReport                  # combined coverage for the 3 library modules
```

Running `maps-app` requires a Maps API key: put `MAPS_API_KEY=...` in
`secrets.properties` at the repo root (see `local.defaults.properties` for the
template). Never hardcode or commit API keys.

## Code style

- Follow the [Jetpack Compose API guidelines](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md) strictly for any public API.
- Composable functions are PascalCase; the first optional parameter of a
  composable is `modifier: Modifier = Modifier`.
- Prefer the library's state holders (`rememberCameraPositionState`,
  `MarkerState`) over ad-hoc state.
- KDoc on all public classes, properties, and functions.
- Avoid `!!`; use null-safe operators.
- Public API changes must be additive and backward compatible; deprecate
  before removing.

## Pull requests

- Use Conventional Commit messages (`feat:`, `fix:`, `docs:`, ...).
  release-please parses them to generate versions and CHANGELOG.md; a wrong
  prefix causes a wrong release bump. Never edit CHANGELOG.md by hand.
- Every behavior change needs a unit test in the affected module.
- Run the module's tests and `lint` before declaring work done, and report
  actual results.
- Do not add dependencies to the library modules without discussion in an
  issue first; the libraries are consumed by many apps and dependency weight
  matters.
- AI tools must not be listed as authors or co-authors on commits or PRs, and
  unsolicited bot-generated PRs are prohibited (see CONTRIBUTING.md).
