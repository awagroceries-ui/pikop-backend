# Task List - Fix Onboarding Errors & KYC Sync

- [ ] Backend: Add unique constraint on `fulfillers(user_id)` via migration
- [ ] Backend: Update `updateFulfillerProfile` to clear orphan records before UPSERT
- [ ] Backend: Update `handlePremblyWebhook` to move `kyc_status` forward
- [ ] Android: Refactor Date Picker to be fully active/clickable
- [ ] Android: Polish advancement logic and refresh handling
- [ ] Verification: Build and verify the entire activation flow
- [ ] Verification: Git automation (Commit and Push)
