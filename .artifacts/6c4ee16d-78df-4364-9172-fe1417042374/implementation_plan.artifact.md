# Implementation Plan - Signup & Legal Service Restoration

This plan addresses the 500 errors occurring during user signup and when accessing the legal pages.

## 🔍 Diagnostic Summary
- **Signup Error**: Most likely caused by the missing `user_legal_consents` table on the production server. The code expects this table to record user agreements, and if the migration hasn't been run, the database query fails.
- **Legal Page Error**: Likely caused by an EJS rendering issue or layout conflict. The generic "An unexpected error occurred" message confirms it's hitting the global error handler.

## Proposed Changes

### 1. Robust Legal Rendering
#### [MODIFY] [legalController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/legalController.js)
- Convert the functions to `async` for better error handling compatibility.
- Explicitly set `layout: false` if we want to bypass the layout engine, or ensure the layout is correctly resolved.
- *Correction*: I will continue using `public_layout` but ensure it's robust and all variables are passed.

### 2. Gated Consent Recording
#### [MODIFY] [authController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/authController.js)
- Wrap the `user_legal_consents` insertion in a sub-try-catch block. This ensures that even if the legal recording fails (e.g., table missing), the primary signup process (user creation) still succeeds. We can log the error as a warning instead of a crash.

### 3. Financial Config Fallback
#### [MODIFY] [commerceController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/commerceController.js)
- Ensure that if the `settings` table is missing the new commission keys, it falls back to hardcoded defaults (10%, 5%, 10%) instead of throwing a parsing error.

## Verification Plan
1.  **Manual Check**: I will advise the user to run `npm run migrate:up` on the server immediately.
2.  **Code Check**: Verify that `authController.js` no longer throws a hard error if the consent table is missing.
3.  **URL Check**: Verify `/legal/privacy` returns a valid page even if the layout is tricky.

## User Action Required
> [!IMPORTANT]
> **Action Needed on Server**
> Please run the following commands on your production VPS immediately to apply the latest database changes:
> ```bash
> cd /var/www/pikop-api/backend_v3/backend_v3
> git pull origin main
> npm run migrate:up
> pm2 restart pikop-v3
> ```
> The 500 error on signup is almost certainly due to the missing `user_legal_consents` table which is created by the latest migration.
