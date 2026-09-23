# Implementation Plan - Order Dispatch Visibility & Merchant Verification Fixes

This plan resolves the Fulfiller order offer query restrictions and fixes the missing `user_id` column error in `kyc_documents` during Merchant signup verification.

## Proposed Changes

### 1. Fulfiller Order Offer Visibility
#### [MODIFY] [fulfillerController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/fulfillerController.js)
- **Select State**: Include `current_state` in initial Fulfiller lookup:
  `SELECT id, online_status, primary_class, current_state FROM fulfillers WHERE user_id = $1`
- **Expand Order Statuses**: Expand `getAvailableOffers` order status filter from `['SEARCHING', 'PAYMENT_CAPTURED']` to `['SEARCHING', 'PAYMENT_CAPTURED', 'PAID', 'CONFIRMED', 'PENDING']`.
- **Null Safety Guards**:
  - Handle null `required_fulfiller_classes`:
    `(o.required_fulfiller_classes IS NULL OR cardinality(o.required_fulfiller_classes) = 0 OR f.primary_class = ANY(o.required_fulfiller_classes))`
  - Handle null or stale `last_ping_at`:
    `(f.last_ping_at IS NULL OR f.last_ping_at > NOW() - interval '2 hours')`
  - Fallback location handling for `f.current_location` when `ST_DWithin` is evaluated.

---

### 2. Merchant Signup & KYC Document Schema Fix
#### [NEW] [1726890000000_add_user_id_to_kyc_documents.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1726890000000_add_user_id_to_kyc_documents.js)
- Create migration to:
  1. Make `fulfiller_id` NULLABLE in `kyc_documents`.
  2. Add `user_id` integer column referencing `users(id) ON DELETE CASCADE`.

#### [MODIFY] [merchantController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/merchantController.js)
- Ensure CAC and NAFDAC document inserts use the new `user_id` column in `kyc_documents` without throwing SQL missing column errors.

---

## User Review Required

> [!IMPORTANT]
> **Database Migration**
> Executing this fix requires applying migration `1726890000000_add_user_id_to_kyc_documents.js` on the database server.

> [!NOTE]
> **Deployment Requirement**
> Deploying these updates requires running `git pull origin main && npm run migrate:up && pm2 restart pikop-v3` on your VPS server.

---

## Verification Plan

### Automated Tests
- Verify Node.js syntax for all modified controller files using `node -c`.
- Execute test script against `getAvailableOffers` and `setupMerchantProfile` endpoints.

### Manual Verification
1. **Fulfiller Dashboard**: Create a new dispatch or commerce order as a Customer. Switch to Fulfiller app and verify the new order request appears under "Available Offers".
2. **Merchant Verification**: Sign up as a new Merchant/Food Vendor, enter CAC / NAFDAC numbers, and tap "SUBMIT FOR VERIFICATION". Verify setup completes cleanly and displays "Business setup submitted successfully".
