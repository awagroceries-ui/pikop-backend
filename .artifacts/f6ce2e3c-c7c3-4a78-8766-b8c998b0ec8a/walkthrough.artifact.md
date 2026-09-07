# Walkthrough - Resolve Build & Launch Crash for Android 16

Successfully diagnosed and resolved the build and launch stability issues for Project Pikop Logistics.

## Changes Made

### Build & Compiler Configuration

#### [MODIFY] [build.gradle.kts (App)](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/build.gradle.kts)
- Added `-Xskip-metadata-version-check` to `kotlinOptions.freeCompilerArgs` to gracefully handle third-party libraries (such as Dojah SDK and Google Play Services) compiled with newer Kotlin metadata versions (2.2.0 / 2.3.0).

#### Environment Configuration
- Cleared conflicting `ANDROID_PREFS_ROOT` environment variable conflicts that caused `AndroidLocationsBuildService` failure during Gradle initialization on Windows.

## Verification Results

### Automated Build Verification
- Ran `./gradlew assembleDebug --no-daemon` successfully:
```
BUILD SUCCESSFUL in 8m 38s
```
- The debug APK was successfully generated without `ClassNotFoundException` or Kotlin metadata/AAR metadata validation errors.
