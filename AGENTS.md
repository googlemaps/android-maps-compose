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
./gradlew test jacocoTestReport                  # all unit tests and code coverage
./gradlew lint                                   # Android Lint across all modules
./gradlew :maps-app:validateDebugScreenshotTest  # validate screenshot tests
```

Running `maps-app` requires a Maps API key: put `MAPS_API_KEY=...` in
`secrets.properties` at the repo root (see `local.defaults.properties` for the
template). Never hardcode or commit API keys.

## Code style

- Adhere to formatting rules defined in `.editorconfig`.
- Do not use wildcard imports (`import foo.*`); use explicit imports.
- Avoid fully qualified class names in source code whenever possible; declare explicit imports at the file level instead (except to resolve naming collisions).
- Library modules compile with `-Xexplicit-api=strict`. All public classes, functions, and properties must declare explicit visibility (`public`) and explicit return types.
- Published library modules target **Java 11** (`JvmTarget.JVM_11`, `JavaVersion.VERSION_11`). Do NOT upgrade the bytecode target to Java 17 or 21, to prevent breaking downstream consumers on AGP < 8.0 with Kotlin inlining errors.
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
- All pull requests are to be created as drafts (`gh pr create --draft`) until authorization is explicitly given to mark them ready for review. Always inform the user that the PR was created as a draft.
- Run the module's tests and `lint` before declaring work done, and report
  actual results.
- Do not add dependencies to the library modules without discussion in an
  issue first; the libraries are consumed by many apps and dependency weight
  matters.
- AI tools must not be listed as authors or co-authors on commits or PRs, and
  unsolicited bot-generated PRs are prohibited (see CONTRIBUTING.md).
