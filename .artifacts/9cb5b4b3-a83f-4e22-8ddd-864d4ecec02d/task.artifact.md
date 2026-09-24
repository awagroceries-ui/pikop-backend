# 📌 Task Checklist: Account Self-Deletion Transaction Fix & Missing Column

- `[/]` Task 1: Database Migration
  - `[ ]` Create database migration `1726930000000_add_kyc_provider_ref_to_users.js`

- `[ ]` Task 2: Backend Controller Fix (`authController.js`)
  - `[ ]` Add `safeExec` helper function
  - `[ ]` Update `deleteAccount` to use `safeExec` for cleanup queries

- `[ ]` Task 3: Git Automation & VPS Deployment
  - `[ ]` Stage, commit, and push changes to GitHub `main`
  - `[ ]` Provide VPS deployment command prompts
