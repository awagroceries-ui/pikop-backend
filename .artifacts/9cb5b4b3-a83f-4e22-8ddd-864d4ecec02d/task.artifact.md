# 📌 Task Checklist: Account Self-Deletion Transaction Fix & Missing Column

- `[x]` Task 1: Database Migration
  - `[x]` Create database migration `1726930000000_add_kyc_provider_ref_to_users.js`

- `[x]` Task 2: Backend Controller Fix (`authController.js`)
  - `[x]` Add `safeExec` helper function
  - `[x]` Update `deleteAccount` to use `safeExec` for cleanup queries

- `[x]` Task 3: Git Automation & VPS Deployment
  - `[x]` Stage, commit, and push changes to GitHub `main`
  - `[x]` Provide VPS deployment command prompts
