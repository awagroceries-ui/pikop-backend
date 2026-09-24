# 📌 Task Checklist: Fulfiller Onboarding State & City Pickers

- `[x]` Task 1: Update API Signup Models & Backend Controller
  - `[x]` Add `current_state` to `SignupRequest` in `ApiService.kt`
  - `[x]` Update `authController.js` (`signup`) to accept and insert `current_state` into `fulfillers`

- `[x]` Task 2: Implement State & City Pickers in `SignupFulfillerScreen.kt`
  - `[x]` Define `nigeriaLocations` map of Nigeria States and cities
  - `[x]` Add State Dropdown Selector (`ExposedDropdownMenuBox`)
  - `[x]` Add City Dropdown Selector (`ExposedDropdownMenuBox`) filtered by selected state
  - `[x]` Pass `current_state` and formatted `home_address` to `apiService.signup()`

- `[x]` Task 3: Build & Deploy to Connected Device
  - `[x]` Build debug APK (`app:assembleDebug`)
  - `[x]` Install and launch on device (`192.168.1.2:42447`)

- `[x]` Task 4: Git Automation & VPS Deployment
  - `[x]` Stage, commit, and push changes to GitHub `main`
  - `[x]` Provide VPS deployment command prompts
