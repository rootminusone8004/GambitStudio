# Build Information

## Project Information
  - **Project Name:** GambitStudio
  - **Package Name:** com.mhrahmankwoshik.gambitstudio

## Build Environment
  - **Android Studio / IntelliJ IDEA (Community Edition):** Ladybug / Iguana or later (IDEA 2024.3+)
  - **Gradle Version:** 8.12
  - **Kotlin Version:** 2.0.21
  - **Java Version:** 17 (Source & Target compatibility), JDK 21 (Build)

## SDK and Tools
  - **compileSdkVersion:** 34
  - **buildToolsVersion:** 34.0.0
  - **minSdkVersion:** 24
  - **targetSdkVersion:** 34

## Gradle
  - **AGP Version:** 8.7.3

## Dependencies
  - `androidx.appcompat:appcompat:1.7.0`
  - `com.google.android.material:material:1.12.0`
  - `androidx.activity:activity-ktx:1.9.3`
  - `androidx.constraintlayout:constraintlayout:2.2.0`
  - `androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7`
  - `androidx.lifecycle:lifecycle-livedata-ktx:2.8.7`
  - `androidx.lifecycle:lifecycle-runtime-ktx:2.8.7`
  - `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0`
  - `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0`

## Build Status
  - **Success:** Yes

---

## Build Instructions

### Terminal

1. **Install prerequisites**
   - Java Development Kit (JDK 17 or 21)
   - Android SDK (API 34)

2. **Open the project in terminal**
   ```bash
   cd GambitStudio
   ```

3. **Run unit tests**
   ```bash
   ./gradlew testDebugUnitTest
   ```

4. **Build Debug APK**
   ```bash
   ./gradlew assembleDebug
   ```
   The debug APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

5. **Build Release APK**
   ```bash
   ./gradlew assembleRelease
   ```
   The release APK will be generated at `app/build/outputs/apk/release/app-release.apk`. It is signed with `app/release.jks` when that file exists, and with the debug key otherwise.

6. **Install via ADB**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

### Fastlane

The same tasks are available as fastlane lanes:

```bash
fastlane test      # Run unit tests
fastlane build     # Build the debug APK
fastlane release   # Build the release APK
fastlane beta      # Clean, then build the release APK
```

---

## Releases & Versioning

Releases are driven by Git tags:

- Pushing a tag such as `v1.2.0` runs `.github/workflows/release.yml`.
- The workflow computes `versionCode = (major * 10000) + (minor * 100) + patch`, runs the unit tests, builds the signed release APK and publishes a GitHub Release.
- The release notes are taken from the top entry of [CHANGELOG.md](CHANGELOG.md).

### README release badge

A `post-commit` Git hook keeps the README release badge in sync. When a commit's message is a release version (for example `v1.2.0`), the hook runs `.github/scripts/update_readme_badge.py` and folds the updated `README.md` into the same commit.

Enable the hook once per clone:

```bash
git config core.hooksPath .githooks
```
