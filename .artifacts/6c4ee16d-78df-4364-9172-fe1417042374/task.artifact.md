# Task: Execute Fulfiller Conduct Policy Web Route & Business Account Setup Fixes

- [ ] **1. Fulfiller Terms & Conduct Web Route Resolution (`legalRoutes.js`, `legalController.js`, `app.js`)**
    - [ ] Add `/fulfiller`, `/terms/fulfiller`, `/terms-fulfiller` in `legalRoutes.js`
    - [ ] Add direct top-level fallbacks for `/terms/fulfiller` in `app.js`
    - [ ] Update `getFulfillerTerms` in `legalController.js`
- [ ] **2. Business Account Setup Fix (`corporateController.js` & Migration)**
    - [ ] Create migration `1726880000000_add_unique_constraint_to_corporate_accounts.js`
    - [ ] Replace `ON CONFLICT` in `corporateController.js` with explicit SELECT/UPDATE/INSERT
    - [ ] Set `is_active = true` on corporate account creation
- [ ] **Verification & Deployment**
    - [ ] Verify JS syntax using `node -c`
    - [ ] Build release App Bundle (`app-release.aab`)
    - [ ] Install release APK on connected Samsung Galaxy device
    - [ ] Commit and push all changes to GitHub
