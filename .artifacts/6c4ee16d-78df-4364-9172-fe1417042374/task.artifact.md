# Task Checklist: Signup Restoration & Transaction Resilience

- `[x]` **1. Database Schema Fixes**
  - `[x]` Create migration `1726450000000_fix_signup_constraints.js`.
- `[x]` **2. Backend Logic Restoration**
  - `[x]` Standardize fulfiller category to lowercase in `authController.js`.
  - `[x]` Move `user_legal_consents` recording after `COMMIT`.
  - `[x]` Enhance signup error logging.
- `[ ]` **3. Verification & Deployment**
  - `[ ]` Build and verify.
  - `[ ]` Push to Git.
