# 📌 Task Checklist: Fix Hotspot Map Camera Center & City/State GPS Location Fallback

- `[/]` Task 1: Add City/State Coordinate Resolver & Permission Launcher (`FulfillerDashboardScreen.kt`)
  - `[ ]` Define `getCityStateCoordinates(stateOrAddress: String?)` mapping all 16+ Nigeria regions
  - `[ ]` Add runtime location permission request launcher (`RequestMultiplePermissions`)
  - `[ ]` Derive city fallback coordinates from `profileData?.current_state` or `profileData?.home_address` and center map camera on agent's city
  - `[ ]` Animate map camera to exact device GPS coordinates when `agentLocation` is resolved

- `[ ]` Task 2: Build & Deploy to Device
  - `[ ]` Build debug APK (`app:assembleDebug`)
  - `[ ]` Install and launch on device (`192.168.1.2:42447`)

- `[ ]` Task 3: Git Automation
  - `[ ]` Stage, commit, and push changes to GitHub `main`
