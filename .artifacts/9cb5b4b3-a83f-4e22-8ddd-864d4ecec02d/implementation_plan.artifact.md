# 📋 Implementation Plan: Fix Hotspot Map Camera Center & City/State GPS Location Fallback

Fix the issue where the Hotspot Map defaults and stays locked to Lagos for non-Lagos agents (e.g. Port Harcourt, Abuja, Ibadan, Kano, etc.).

---

## 🔍 Root Cause Analysis

1. **Hardcoded Lagos Initial Camera Position**:
   - `cameraPositionState` in `FulfillerDashboardScreen.kt` initialized with `agentLocation ?: LatLng(6.5244, 3.3792)` (Lagos). Because `agentLocation` starts as `null`, the map camera immediately positioned itself at Lagos coordinates.
2. **Silent GPS Failure & Missing Runtime Permission Request**:
   - `fusedLocationClient` location fetch failed silently when location permissions were not explicitly requested at runtime on the Dashboard screen. When `agentLocation` remained `null`, the map stayed permanently locked to Lagos.
3. **Absence of Agent Profile City/State Fallback**:
   - The map did not inspect the agent's profile (`profileData?.current_state` or `profileData?.home_address`) to derive city/state default coordinates before or in the absence of a device GPS fix.

---

## 🛠️ Proposed Changes

### Component 1: Android Mobile App (`:app`)

#### [MODIFY] [FulfillerDashboardScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/FulfillerDashboardScreen.kt)
- Add **Nigeria State & City Coordinate Resolver**:
  - `getCityStateCoordinates(stateOrAddress: String?)`: Returns city/state center coordinates for all Nigeria regions:
    - **Rivers / Port Harcourt**: `LatLng(4.8156, 7.0498)`
    - **FCT / Abuja**: `LatLng(9.0765, 7.3986)`
    - **Lagos / Ikeja**: `LatLng(6.5244, 3.3792)`
    - **Oyo / Ibadan**: `LatLng(7.3775, 3.9470)`
    - **Kano**: `LatLng(12.0022, 8.5919)`
    - **Delta / Asaba / Warri**: `LatLng(6.2059, 6.6959)`
    - **Anambra / Awka / Onitsha**: `LatLng(6.2209, 7.0670)`
    - **Enugu**: `LatLng(6.4584, 7.5464)`
    - **Edo / Benin**: `LatLng(6.3350, 5.6037)`
    - **Akwa Ibom / Uyo**: `LatLng(5.0377, 7.9128)`
    - **Cross River / Calabar**: `LatLng(4.9757, 8.3417)`
    - **Ogun / Abeokuta**: `LatLng(7.1475, 3.3619)`
    - **Kaduna**: `LatLng(10.5105, 7.4165)`
    - **Imo / Owerri**: `LatLng(5.4832, 7.0358)`
    - **Abia / Aba / Umuahia**: `LatLng(5.1066, 7.3667)`
    - **Plateau / Jos**: `LatLng(9.8965, 8.8583)`
- Update camera initialization:
  - When `profileData` loads, immediately derive city fallback coordinates from `profileData?.current_state` or `profileData?.home_address`. If `agentLocation` is null, animate map camera to their city center (e.g. Port Harcourt `4.8156, 7.0498` for Rivers agents).
- Add runtime **Location Permission Request Launcher**:
  - Automatically requests `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` on screen launch. Upon permission grant, immediately triggers `resolveAgentLocation()` to update `agentLocation` to exact device GPS coordinates and animate camera.

---

## 🧪 Verification Plan

### Automated & Manual Verification
1. Test Dashboard map launch on connected **Samsung Galaxy S23 Ultra** (`192.168.1.2:42447`).
2. Verify an agent registered in `Rivers / Port Harcourt` immediately sees the map centered at Port Harcourt (`4.8156, 7.0498`) on dashboard open, NOT Lagos.
3. Test GPS location resolution: Verify the blue **"Your Location"** marker updates to the device's exact GPS location and camera centers seamlessly.
4. Rebuild debug APK (`gradle_build("app:assembleDebug")`) and deploy to device.
5. Stage, commit, and push changes to GitHub `main`.
