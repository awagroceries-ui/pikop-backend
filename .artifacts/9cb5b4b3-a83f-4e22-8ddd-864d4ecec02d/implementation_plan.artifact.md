# 📋 Implementation Plan: Fulfiller Onboarding Nigeria State & City Picker

Add interactive Nigeria State and City dropdown pickers/selectors to the Fulfiller/Agent onboarding form (`SignupFulfillerScreen.kt`) and sync `current_state` and `home_address` with the backend API.

---

## 🔍 Requirements & Research Analysis

1. **Current State in `SignupFulfillerScreen.kt`**:
   - `operatingCity` is currently a plain text field (`OutlinedTextField`), where users type city names manually without state context or standardized dropdown selection.
2. **Nigeria State & City Mapping**:
   - Standardized mapping of 16+ Nigeria States and major cities (`Lagos`, `Rivers`, `FCT (Abuja)`, `Oyo`, `Kano`, `Delta`, `Edo`, `Anambra`, `Enugu`, `Kaduna`, `Ogun`, `Akwa Ibom`, `Abia`, `Cross River`, `Imo`, `Plateau`, etc.).
3. **Integration & Dispatch**:
   - Selecting a State (e.g., `Rivers`) filters the available Cities list (e.g., `Port Harcourt`, `Obio-Akpor`, `Eleme`, `Bonny`, `Onne`).
   - `SignupRequest` model in `ApiService.kt` will send `current_state` (State name) and `home_address` (`"$homeAddress, $operatingCity, $operatingState State"`).
   - Backend `authController.js` (`signup`) will accept `current_state` and store it in `fulfillers.current_state` for immediate dispatch and city rule matching.

---

## 🛠️ Proposed Changes

### Component 1: Android Mobile App (`:app`)

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Add `current_state: String? = null` to `SignupRequest` data class.

#### [MODIFY] [SignupFulfillerScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/SignupFulfillerScreen.kt)
- Add `operatingState` and `operatingCity` dropdown state handlers (`ExposedDropdownMenuBox`).
- Define `nigeriaLocations` map containing Nigeria States and their major cities.
- Add **State Dropdown Selector** field (`ExposedDropdownMenuBox`).
- Add **City Dropdown Selector** field (`ExposedDropdownMenuBox`) that dynamically populates based on the selected State.
- Pass `current_state = operatingState` and formatted `home_address` to `apiService.signup()`.

---

### Component 2: Backend API Controller

#### [MODIFY] [authController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/authController.js)
- Accept `current_state` in `signup` request body.
- Update `INSERT INTO fulfillers (..., current_state)` to save `current_state` upon registration.

---

## 🧪 Verification Plan

### Manual & Device Testing
1. Compile debug APK (`gradle_build("app:assembleDebug")`).
2. Deploy to connected device (`192.168.1.2:42447`).
3. Open Fulfiller Signup screen (`SignupFulfillerScreen.kt`).
4. Tap **Operating State** dropdown -> select `Rivers` (or `Lagos` / `FCT (Abuja)`).
5. Tap **Operating City** dropdown -> verify the city list populates with cities for the selected State (e.g. `Port Harcourt`, `Obio-Akpor`, `Eleme`).
6. Complete signup -> verify request sends `current_state` and `home_address` and creates the fulfiller account successfully.
7. Stage, commit, and push to GitHub `main`.
8. Provide VPS deployment commands for server update.
