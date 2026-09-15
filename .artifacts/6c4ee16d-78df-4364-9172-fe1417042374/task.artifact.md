# Task Checklist: Customized Welcome Emails

- `[/]` **Backend Infrastructure**
  - `[x]` Confirm Brevo SMTP in `emailService.js`.
- `[/]` **Tailored Templates (`emailService.js`)**
  - `[ ]` Refactor `sendWelcomeEmail` to support `CUSTOMER`, `FULFILLER`, `FOOD_MERCHANT`, `GROCERIES_MERCHANT`, and `SHOP_MERCHANT`.
  - `[ ]` Add dynamic blocks for commission rates, categories, and COD settings.
- `[/]` **Trigger Gating (`authController.js`)**
  - `[ ]` Update `verifyOtp` to only send email immediately for `CUSTOMER`.
- `[/]` **Approval Triggers (`adminController.js`)**
  - `[ ]` Update `updateKYCStatus` to trigger fulfiller/merchant welcome emails upon verification.
- `[ ]` **Verification**
  - `[ ]` Verify logic correctly routes categories.
  - `[ ]` Git commit and push changes.
