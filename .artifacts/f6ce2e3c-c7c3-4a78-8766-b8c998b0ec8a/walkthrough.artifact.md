# Walkthrough - Legal Compliance & Help Center Population

I have strengthened the platform's legal compliance and fully populated the Help Center with role-specific FAQs to improve user onboarding and support.

## Key Improvements

### 1. Mandatory Legal Acceptance
- **Signup Screen:** Added a mandatory "I accept the Terms & Conditions and Privacy Policy" checkbox to the initial signup form. Users cannot proceed until they explicitly agree.
- **Verification Screen:** Updated the post-OTP "Terms" screen to also include an explicit combined acceptance checkbox, ensuring compliance at the point of account activation.

### 2. Permanent Policy Accessibility
- **Account Menu:** Added "Terms & Conditions" and "Privacy Policy" links directly to the **Account/Profile** menu.
- **Always Available:** Users can now reference the official platform rules at any time without having to sign out.
- **Dynamic Content:** Refactored the Privacy Policy screen (matching the T&C screen) to load its content live from your server. Any policy updates you make on the backend will instantly reflect in the app.

### 3. Populated Help Center (FAQs)
- **Role-Based Content:** Seeded the database with professional FAQs tailored to the user's account type.
- **Topics Covered:**
    - **App Navigation:** How to place orders and how agents go online.
    - **Earnings:** Clarity on the **75/25 split** for agents.
    - **Wallet & Withdrawals:** How to fund accounts and how agents can withdraw earnings to their banks.
    - **Policies:** Simplified explanations of the **25% cancellation penalty** and **75% return fee**.

### 4. Comprehensive Legal & Protective Framework
- **Overhauled T&C:** Implemented a professional legal framework with a robust **Hold Harmless (Indemnification)** clause to protect Pikop and its independent agents from liability.
- **Prohibited Items Discovery:** Added strict language stating that any prohibited items discovered during transit will be reported to the police along with sender details, and the item will be discarded immediately without refund.
- **Jurisdiction:** Standardized all legal disputes to the courts of **Port Harcourt, Rivers State**.

### 5. Branded Communication Update
- **Welcome Emails:** Updated the welcome email summary to include the 25% cancellation fee, 75% return fee, and the new prohibited items reporting clause.
- **Regional Hub:** Updated the global email footer from Lagos to **"Port Harcourt, Nigeria"**.

### Backend Updates
- Created migration `1725610000000_seed_kb_faqs.js`.
- Verified `legalController.js` serves both T&C and Privacy HTML content.
- **Result:** `PASS`.

### Android Build
- Integrated new navigation callbacks and refactored UI components.
- Ran `./gradlew assembleDebug`.
- **Result:** `BUILD SUCCESSFUL`.

## Deployment Instructions (VPS)
Please apply these legal and content updates to your **VPS**:

```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
npm run migrate:up
pm2 restart pikop-v3
```
