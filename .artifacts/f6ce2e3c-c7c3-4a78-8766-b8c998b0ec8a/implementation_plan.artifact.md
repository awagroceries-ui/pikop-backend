# Implementation Plan - Fix Map Rendering & Auto-Suggest in Order Requests

## Problem Description

Users reported two related issues in order request screens:
1. **Map not rendering for order requests**: `GoogleMap` in `MapAddressSearchScreen.kt` relies on the Google Maps API key (`com.google.android.geo.API_KEY`) and Google Play Services. If `Places` or Maps initialization is delayed or asynchronous, rendering can fail or show blank tiles.
2. **Map search bar auto-suggest not functioning**: `MapAddressSearchScreen.kt` uses the native `PlacesClient` (`placesClient.findAutocompletePredictions(...)`), which requires `Places.initialize(...)` to have completed successfully. Because initialization was previously deferred on a background coroutine (`GlobalScope.launch`), navigating to `MapAddressSearchScreen` immediately can trigger `Places` uninitialized errors, causing auto-suggest to fail.

## User Review Required

> [!IMPORTANT]
> This plan ensures synchronous or safe initialization of Google Places in `PikopApp.kt` and adds robust error handling and fallback mechanisms in `MapAddressSearchScreen.kt`.

## Proposed Changes

### Application Initialization

#### [MODIFY] [PikopApp.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/PikopApp.kt)
- Initialize Google Places synchronously in `PikopApp.onCreate()` (or ensure it is initialized immediately) so that `Places.isInitialized()` is always true across all screens.

### Map & Search Screen

#### [MODIFY] [MapAddressSearchScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/app/src/main/java/com/ng/pikop/feature/order/MapAddressSearchScreen.kt)
- Add a safety check in `MapAddressSearchScreen` to initialize `Places` if not already initialized using `BuildConfig.GOOGLE_MAPS_API_KEY`.
- Improve error logging and graceful handling for autocomplete predictions and place details fetching.

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug --no-daemon` to ensure compilation succeeds.

### Manual Verification
- Deploy the app to a connected device or emulator, open the delivery request order flow, tap search/map, and verify that Google Map renders correctly and the search bar auto-suggest returns predictions.
