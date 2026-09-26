# 📌 Task Checklist: Fix Invisible Text & High-Contrast Theme Adaptation

- `[/]` Task 1: Refactor `ServiceButton` in `CustomerHomeScreen.kt`
  - `[ ]` Update container color to `MaterialTheme.colorScheme.surfaceVariant`
  - `[ ]` Set title text color to `MaterialTheme.colorScheme.onSurface` (bold crisp white in dark mode, dark slate in light mode)
  - `[ ]` Set subtitle text color to `MaterialTheme.colorScheme.onSurfaceVariant`
  - `[ ]` Set icon tint to `MaterialTheme.colorScheme.primary`
  - `[ ]` Update helpful tip card text color to `MaterialTheme.colorScheme.onSurfaceVariant`

- `[ ]` Task 2: Update Order Summary & Offer Cards (`OrderQuoteScreen.kt` & `IncomingOfferComponent.kt`)
  - `[ ]` Update `SummaryLine` labels and `LocationInput` labels in `OrderQuoteScreen.kt`
  - `[ ]` Update Pickup/Dropoff label titles and address text in `IncomingOfferComponent.kt`

- `[ ]` Task 3: Build & Deploy to Device
  - `[ ]` Build debug APK (`app:assembleDebug`)
  - `[ ]` Install and launch on device (`192.168.1.2:42447`)

- `[ ]` Task 4: Git Automation
  - `[ ]` Stage, commit, and push changes to GitHub `main`
