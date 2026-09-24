# 📌 Task Checklist: Account Deletion UX & Wallet Withdraw Button Fix

- `[x]` Task 1: Backend Deletion Message Clarification (`authController.js`)
  - `[x]` Update `deleteAccount` to accurately state if the funds are Pending in escrow vs Available balance

- `[x]` Task 2: Mobile App Wallet Screen UI (`WalletScreen.kt`)
  - `[x]` Make the "Withdraw" button always visible, but disabled when `balance <= 0`

- `[x]` Task 3: Build & Deploy to Device
  - `[x]` Build debug APK (`app:assembleDebug`)
  - `[x]` Install and launch on device (`192.168.1.2:42447`)

- `[x]` Task 4: Git Automation & VPS Deployment
  - `[x]` Stage, commit, and push changes to GitHub `main`
  - `[x]` Provide VPS deployment command prompts
