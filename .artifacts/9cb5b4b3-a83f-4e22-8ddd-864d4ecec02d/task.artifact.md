# 📌 Task Checklist: Redesign Role Selection Screen ("How do you want to use Pikop?")

- `[/]` Task 1: Copy PNG Icon Assets
  - `[ ]` Copy `role_icon_send.png` to `app/src/main/res/drawable/`
  - `[ ]` Copy `role_icon_earn.png` to `app/src/main/res/drawable/`
  - `[ ]` Copy `role_icon_sell.png` to `app/src/main/res/drawable/`
  - `[ ]` Copy `role_icon_fleet_partner.png` to `app/src/main/res/drawable/`
  - `[ ]` Copy `role_icon_business_account.png` to `app/src/main/res/drawable/`

- `[ ]` Task 2: Redesign `UserTypeSelectionScreen.kt`
  - `[ ]` Keep Pikop logo `R.drawable.pikop_logo` 100% untouched
  - `[ ]` Format heading: "How do you want to use " (white) + "Pikop?" (green `#00E676`)
  - `[ ]` Add subtitle: "Choose your experience" (`#9CA3AF`)
  - `[ ]` Upgrade `RoleCard` with PNG assets (`95dp`), color-glow borders & subtle background tints
  - `[ ]` Add circular arrow affordance in bottom-right corner of each card
  - `[ ]` Add "FOR ORGANIZATIONS" divider with horizontal rules
  - `[ ]` Restyle login link ("Log in →") with green underline & trailing arrow
  - `[ ]` Add trust signal ("Your data is safe with us" + shield icon)

- `[ ]` Task 3: Build & Deploy to Device
  - `[ ]` Build debug APK (`app:assembleDebug`)
  - `[ ]` Install and launch on device

- `[ ]` Task 4: Git Automation
  - `[ ]` Stage, commit, and push changes to GitHub `main`
