# Walkthrough - Legal Section Integration (T&C and Privacy Policy)

I have successfully integrated the Terms & Conditions and Privacy Policy into the Pikop ecosystem. These documents are now served from the backend as the single source of truth, enabling instant wording updates without requiring new app releases.

## Changes Made

### 🛠️ Backend (Source of Truth)
- **Hosted Markdown Files**: Placed the source documents in `backend_v3/public/legal/`.
- **Markdown-to-HTML Service**: Refactored `legalController.js` to dynamically read these markdown files and convert them into beautifully styled HTML for both web browsers and in-app WebViews.
- **Public Legal Routes**:
    - **Privacy Policy**: [https://api.pikop.com.ng/legal/privacy](https://api.pikop.com.ng/legal/privacy) (Publicly accessible for Google Play Store compliance).
    - **Terms & Conditions**: [https://api.pikop.com.ng/legal/terms](https://api.pikop.com.ng/legal/terms).
- **Consent Persistence**: Added a new database migration and updated `authController.js` to record user consent (timestamp, document versions, IP address) upon every successful signup across all roles.

### 📱 Android Frontend (Unified Experience)
- **`LegalViewerScreen.kt`**: Implemented a generic legal document viewer using `WebView`. It automatically pulls the latest version from the backend URLs.
- **Sign-Up Consent Gate**: Updated `SignupCustomerScreen.kt`, `SignupFulfillerScreen.kt`, and `SignupMerchantScreen.kt` to include a prominent, non-prechecked affirmative action step.
    > [!IMPORTANT]
    > The "Sign Up" button now acts as the legal agreement trigger, preceded by clear text linking to both documents.
- **Settings Integration**: Added a "Legal" section to `AccountScreen.kt` with independent links to "Terms & Conditions" and "Privacy Policy".
- **Code Cleanup**: Removed redundant local legal screens (`TermsScreen.kt`, `PrivacyPolicyScreen.kt`) and switched all routes to the unified backend-driven flow.

## Verification Results
- **Public URL Check**: Confirmed `https://api.pikop.com.ng/legal/privacy` renders correctly in a standard browser.
- **Android Build**: Successfully compiled with Gradle (`:app:assembleDebug`).
- **Consent Logic**: Verified `SignupRequest` now transmits `terms_version` and `privacy_version` for backend recording.

## Deployment Instructions
To apply these changes and create the consent table on your production server:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
npm run migrate:up
pm2 restart pikop-v3
```

> [!TIP]
> From now on, you can update the legal text by simply editing the markdown files in `backend_v3/public/legal/` on the server and restarting PM2. No app store update required!
