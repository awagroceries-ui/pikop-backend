# Task: Production Fixes for Fulfiller Signup & Merchant Account Updates

- [ ] **Fulfiller Signup Fix**
    - [ ] Create migration `1726860000000_make_fulfiller_password_nullable.js`
    - [ ] Update `authController.js` to pass `passwordHash` in `INSERT INTO fulfillers`
- [ ] **Merchant Account Update Fix**
    - [ ] Expand `updateMerchantSettings` in `merchantController.js` to handle `allows_returns`, `return_window_days`, `return_policy_text`, `business_name`, `category`, `address`
    - [ ] Add null-safe fallback for `store_slug` generation
- [ ] **Verification & Deployment**
    - [ ] Verify syntax on modified files using `node -c`
    - [ ] Commit and push changes to GitHub
