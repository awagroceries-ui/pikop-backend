# 📌 Task Checklist: Fulfiller Onboarding State & City Pickers

- `[/]` Task 1: Update API Signup Models & Backend Controller
  - `[ ]` Add `current_state` to `SignupRequest` in `ApiService.kt`
  - `[ ]` Update `authController.js` (`signup`) to accept and insert `current_state` into `fulfillers`

- `[ ]` Task 2: Implement State & City Pickers in `SignupFulfillerScreen.kt`
  - `[ ]` Define `nigeriaLocations` map of Nigeria States and cities
  - `[ ]` Add State Dropdown Selector (`ExposedDropdownMenuBox`)
  - `[ ]` Add City Dropdown Selector (`ExposedDropdownMenuBox`) filtered by selected state
  - `[ ]` Pass `current_state` and formatted `home_address` to `apiService.signup()`

- `[ ]` Task 3: Build & Deploy to Connected Device
  - `[ ]` Build debug APK (`app:assembleDebug`)
  - `[ ]` Install and launch on device (`192.168.1.2:42447`)

- `[ ]` Task 4: Git Automation & VPS Deployment
  - `[ ]` Stage, commit, and push changes to GitHub `main`
  - `[ ]` Provide VPS deployment command prompts
