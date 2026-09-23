# Implementation Plan - Legal Route Fallbacks & Business Account Setup Fix

This plan fixes the "Cannot GET /terms/fulfiller" routing conflict and resolves the PostgreSQL ON CONFLICT constraint error on Business Account "COMPLETE SETUP".

## Proposed Changes

### 1. Fulfiller Terms & Conduct Web Route Resolution
#### [MODIFY] [legalRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/legalRoutes.js)
- Add sub-route handlers `/fulfiller`, `/terms/fulfiller`, `/terms-fulfiller` mapping to `legalController.getFulfillerTerms`.

#### [MODIFY] [app.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/app.js)
- Add direct top-level route fallbacks for `app.get('/terms/fulfiller', ...)` and `app.get('/terms-fulfiller', ...)` so all URL permutations (`/terms/fulfiller`, `/legal/terms/fulfiller`, `/terms-fulfiller`) return `200 OK` with the Fulfiller Conduct Policy.

---

### 2. Business Account Setup & "COMPLETE SETUP" Button Fix
#### [MODIFY] [corporateController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/corporateController.js)
- Fix SQL query in `setupCorporateProfile`:
  - Replace `ON CONFLICT (owner_user_id)` with an explicit `SELECT` check to check if an account exists for `owner_user_id`, executing an `UPDATE` or `INSERT` cleanly without throwing PostgreSQL constraint errors.
  - Set `is_active = true` so the corporate account activates immediately upon setup submission.

#### [NEW] [1726880000000_add_unique_constraint_to_corporate_accounts.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1726880000000_add_unique_constraint_to_corporate_accounts.js)
- Add migration to add `UNIQUE (owner_user_id)` constraint on `corporate_accounts` table.

---

## User Review Required

> [!IMPORTANT]
> **Business Account Verification**
> Submitting the "COMPLETE SETUP" form will now immediately activate the company account, create the corporate wallet, assign the user as `ADMIN` in `corporate_sub_accounts`, and load the Corporate Dashboard without errors.

> [!NOTE]
> **Deployment Requirement**
> Applying these updates requires running `git pull origin main && npm run migrate:up && pm2 restart pikop-v3` on your VPS server.

---

## Verification Plan

### Automated Tests
- Verify Node.js syntax for all modified controller files using `node -c`.
- Test `/terms/fulfiller` endpoint using `curl` / `node`.
- Test corporate setup payload using `node` script.

### Manual Verification
1. **Terms Link**: Tap "Fulfiller Terms" in app. Confirm policy loads without "Cannot GET" error.
2. **Business Account Setup**: Open Corporate Account setup screen, fill company details, tap "COMPLETE SETUP". Confirm setup completes and opens Corporate Dashboard.
