# 📌 Task Checklist: Updated Fee Rates, Welcome Emails & Legal Terms v0.2

- `[x]` Task 1: Seed Dynamic Fee Settings Migration
  - `[x]` Create database migration `1726950000000_update_platform_fees_and_commissions.js` (COD 5%, Dispatch 20%, Merchant 5%)
  - `[x]` Update `PlatformConfig` defaults in `config/platform.js`

- `[x]` Task 2: Update Welcome Emails
  - `[x]` Update `sendWelcomeEmail` in `emailService.js` to query database `settings` dynamically for active fee rates

- `[x]` Task 3: Update Legal Terms v0.2
  - `[x]` Update `Pikop_Terms_and_Conditions.md` to Version 0.2 (September 24, 2026)
  - `[x]` Update Fees Summary Table and sections (COD 5%, Dispatch 20% / 80% Fulfiller share, Merchant 5% across Food/Groceries/Shop)

- `[x]` Task 4: Git Automation & VPS Deployment
  - `[x]` Stage, commit, and push changes to GitHub `main`
  - `[x]` Provide VPS deployment command prompts
