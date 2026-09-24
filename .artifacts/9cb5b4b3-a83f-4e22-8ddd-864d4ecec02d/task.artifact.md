# 📌 Task Checklist: Account Deletion UX & Wallet Withdraw Button Fix

- `[/]` Task 1: Backend Deletion Message Clarification (`authController.js`)
  - `[ ]` Update `deleteAccount` to accurately state if the funds are Pending in escrow vs Available balance

- `[ ]` Task 2: Mobile App Wallet Screen UI (`WalletScreen.kt`)
  - `[ ]` Make the "Withdraw" button always visible, but disabled when `balance <= 0`

- `[ ]` Task 3: Build & Deploy to Device
  - `[ ]` Build debug APK (`app:assembleDebug`)
  - `[ ]` Install and launch on device (`192.168.1.2:42447`)

- `[ ]` Task 4: Git Automation & VPS Deployment
  - `[ ]` Stage, commit, and push changes to GitHub `main`
  - `[ ]` Provide VPS deployment command prompts
