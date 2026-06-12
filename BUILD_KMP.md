# Kotlin Multiplatform (KMP) Maps Demo — Build & Run Guide

This guide details how to build and run the multiplatform Maps Compose library (`:maps-compose-multiplatform`), the Android demo app (`:maps-app`), and the iOS demo app (`iosApp`).

---

## 1. Checkout the PR / Branch

Fetch the remote branch and check it out (or add it as a Git worktree):

```bash
git fetch origin feat/experimental-kmp-module
git checkout feat/experimental-kmp-module
```

*(Alternatively, to isolate your work in a worktree branch)*:
```bash
git worktree add -b feat/experimental-kmp-module feat-experimental-kmp-module origin/feat/experimental-kmp-module
```

---

## 2. Configure Local API Keys

Create a file named `secrets.properties` in the **root** directory of the repository (if you don't already have one) and populate it with your Google Maps API keys:

```properties
MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY
PLACES_API_KEY=YOUR_GOOGLE_PLACES_API_KEY
```

> [!IMPORTANT]
> Both `secrets.properties` and the generated iOS equivalent (`DeveloperSecrets.swift`) are ignored by Git to prevent accidentally leaking credentials.

---

## 3. Building & Running for Android

To compile and verify both the KMP library Android target and the Android sample application (`:maps-app`), run:

```bash
./gradlew assembleDebug
```

---

## 4. Building & Running for iOS (Step-by-Step)

If you are running the iOS project (`iosApp`) for the first time, follow these steps on a Mac with **Xcode** and **CocoaPods** installed:

### Step 4.1: Generate the KMP Dummy Framework
Before CocoaPods can link the local `:maps-compose-multiplatform` podspec, the initial Kotlin framework must exist. From the repository root, execute:

```bash
./gradlew :maps-compose-multiplatform:generateDummyFramework
```

### Step 4.2: Generate the Xcode Project
Navigate to the `iosApp` directory and run the project generation Ruby script (requires the `xcodeproj` gem):

```bash
cd iosApp
ruby create_project.rb
```

> [!NOTE]
> **Build System Output Tracking (Xcode 16+)**: The `create_project.rb` script declares `$(SRCROOT)/iosApp/DeveloperSecrets.swift` as an output of the **Populate Secrets** script build phase. This prevents modern Xcode build systems from failing with `Build input file cannot be found: DeveloperSecrets.swift` during dependency graph analysis.

### Step 4.3: Install Pod Dependencies
Install the required CocoaPods (`GoogleMaps` 10.14.0 and the local KMP library). If your local specs repository is out of date, use the `--repo-update` flag:

```bash
pod install --repo-update
```

### Step 4.4: Open and Run in Xcode
1. Open the generated **Workspace** (do **not** open the `.xcodeproj` file directly):
   ```bash
   open iosApp.xcworkspace
   ```
2. In Xcode's top toolbar, select the active scheme dropdown and set it to **`iosApp`** (instead of `maps_compose_multiplatform`).
3. Choose an iOS Simulator (e.g., **iPhone 16** or **iPhone 17 Pro**) in the destination dropdown.
4. Click the **Play** button (or press **Cmd + R**) to compile, run the secrets population script, and launch the app!
