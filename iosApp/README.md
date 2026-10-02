# iOS demo app

Demos of `maps-compose-multiplatform` on iOS, the counterpart of the multiplatform demo in
`maps-app`. It needs a Mac with Xcode, CocoaPods and the `xcodeproj` gem (installed with
CocoaPods).

## Run it

1. Put your Maps API key in `secrets.properties` at the repository root, as for the Android app:

   ```properties
   MAPS_API_KEY=YOUR_API_KEY
   ```

   A build phase copies it into `iosApp/DeveloperSecrets.swift`, which is ignored by git. Without
   a key the app still builds, but the maps stay blank.

2. From the repository root, create the placeholder framework CocoaPods links against:

   ```bash
   ./gradlew :maps-compose-multiplatform:generateDummyFramework
   ```

3. Generate the Xcode project and install the pods:

   ```bash
   cd iosApp
   ruby create_project.rb
   pod install
   ```

   The project is generated, not checked in. Run `create_project.rb` again after pulling changes
   to it.

4. Open `iosApp.xcworkspace` (not the `.xcodeproj`), select the `iosApp` scheme and an iOS
   simulator, and run. Xcode builds the Kotlin framework through Gradle as part of the build.

To build from the command line, as CI does:

```bash
xcodebuild build -workspace iosApp.xcworkspace -scheme iosApp \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO
```

## Troubleshooting

- **`Build input file cannot be found: DeveloperSecrets.swift`**: the project predates the
  secrets build phase output. Run `ruby create_project.rb` again.
- **Intel Macs**: only Apple silicon simulators are supported. Compose Multiplatform no longer
  publishes the `iosX64` target.
