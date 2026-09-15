# Implementation Plan - Legal Integration (T&C and Privacy Policy)

This plan integrates the Pikop Terms & Conditions and Privacy Policy into the app and backend. The documents will be served from the backend as the single source of truth, enabling wording changes without app releases.

## User Review Required

> [!IMPORTANT]
> **Source of Truth Details**
> I will host the markdown files on the backend (`backend_v3/public/legal/`). The app will fetch the content via API or render them in a WebView via public URLs.
> - **Public URL for Play Store**: `https://api.pikop.com.ng/legal/privacy` (and `/legal/terms` for consistency).

## Proposed Changes

### Backend (Node.js)

#### [NEW] Markdown Files
- Place `Pikop_Terms_and_Conditions.md` and `Pikop_Privacy_Policy.md` in `backend_v3/public/legal/`.

#### [MODIFY] [legalController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/legalController.js)
- Update `getTerms` and `getPrivacyPolicy` to read from the markdown files and render them using the existing `legal_pages.ejs` view.
- I will implement a simple markdown-to-HTML parser (or wrap the text in `<pre>` with `white-space: pre-wrap`) to avoid adding new dependencies if `marked` is not preferred. *Self-correction: I will try to use a basic conversion for better readability.*

#### [NEW] [user_legal_consents migration](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1726430000000_create_user_legal_consents.js)
- Create a table to record user consent: `user_id`, `terms_version`, `privacy_version`, `consented_at`, `ip_address`.

#### [MODIFY] [authController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/authController.js)
- Update the `signup` logic to record the consent in the new table upon account creation.

---

### Android Frontend (Compose)

#### [NEW] `LegalViewerScreen.kt`
- A generic screen using `WebView` to load the legal documents from the backend URLs.

#### [MODIFY] `SignupCustomerScreen.kt`, `SignupFulfillerScreen.kt`, `SignupMerchantScreen.kt`
- Replace the current checkbox with a prominent "By continuing, you agree to..." section.
- Ensure the "Sign Up" button is only enabled after the user has at least opened/scrolled the terms (or just after they click the links). *Instruction check: The prompt says "require an affirmative action", which typically means a button click after reading.*

#### [MODIFY] `AccountScreen.kt` (Settings)
- Add "Terms & Conditions" and "Privacy Policy" menu items under a "Legal" section.

#### [MODIFY] `MainActivity.kt`
- Register the new `LegalViewerScreen` route.

## Verification Plan

### Automated/Code Verification
- Verify backend routes `/legal/terms` and `/legal/privacy` return the correct HTML from markdown.
- Verify `user_legal_consents` table is populated on signup.

### Manual Verification
1.  **Public Access**: Load `https://api.pikop.com.ng/legal/privacy` in a browser.
2.  **In-App Navigation**: Go to Settings -> Legal -> Privacy Policy. Verify it loads.
3.  **Signup Flow**: Create a new test account. Confirm the "By continuing..." text is visible and links work.
4.  **Consent Record**: Check the DB for the new consent entry with the correct version (0.1).
