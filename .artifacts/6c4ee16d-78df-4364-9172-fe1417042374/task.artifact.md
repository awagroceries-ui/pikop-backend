# Task: Fix Fulfiller Order Offer Visibility & Merchant KYC Document Schema

- [ ] **1. Fulfiller Order Offer Visibility (`fulfillerController.js`)**
    - [ ] Select `current_state` in initial Fulfiller query
    - [ ] Expand order status filter in `getAvailableOffers` to include `PAID`, `CONFIRMED`, `PENDING`
    - [ ] Add SQL null-safety guards for `required_fulfiller_classes` and `last_ping_at`
- [ ] **2. Merchant Signup & KYC Document Schema Fix (`merchantController.js` & Migration)**
    - [ ] Create migration `1726890000000_add_user_id_to_kyc_documents.js`
    - [ ] Make `fulfiller_id` NULLABLE and add `user_id` column to `kyc_documents`
    - [ ] Update CAC and NAFDAC document inserts in `merchantController.js`
- [ ] **Verification & Deployment**
    - [ ] Verify JS syntax using `node -c`
    - [ ] Build release App Bundle (`app-release.aab`)
    - [ ] Install release APK on connected Samsung Galaxy device
    - [ ] Commit and push all changes to GitHub
