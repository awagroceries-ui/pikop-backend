# Implementation Plan - Legal Accessibility & FAQ Population

This plan ensures that legal documents are permanently accessible, explicitly accepted after verification, and that the help center is auto-populated with role-specific FAQs.

## User Review Required

> [!IMPORTANT]
> **Acceptance Flow:** Per your request, the explicit "I accept" checkbox will remain on the **Terms Screen** which appears immediately after a user successfully verifies their account (OTP). I will update the text to ensure both Terms and Privacy Policy are explicitly mentioned.
>
> **Dynamic Legal Content:** I will refactor the Privacy Policy screen to fetch its content live from your server. This allows you to update policies once on the server and have them reflected everywhere.

## Proposed Changes

### Backend (`backend_v3`)

#### [NEW] [Migration: seed_kb_faqs.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1725610000000_seed_kb_faqs.js)
- Seed the `knowledge_base` table with structured FAQs covering:
    - **App Navigation:** How to book, how to go online.
    - **Earnings (Agents):** Understanding the 75/25 split.
    - **Wallet & Withdrawals:** How to fund wallet and how to withdraw earnings to a bank.
    - **Policies:** Cancellation fees (25%) and Return fees (75%).

#### [MODIFY] [legalController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/legalController.js)
- Ensure the `getLegalConfig` endpoint returns high-quality HTML for both `terms_html` and `privacy_html`.

---

### Android App

#### [MODIFY] [TermsScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/TermsScreen.kt)
- Update the mandatory checkbox label to: *"I have read and agree to the Terms & Conditions and Privacy Policy of Awa Foods & Groceries."*

#### [MODIFY] [AccountScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/AccountScreen.kt)
- Add two new options: "Terms & Conditions" and "Privacy Policy" to ensure they are always accessible for reference.

#### [MODIFY] [PrivacyPolicyScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/PrivacyPolicyScreen.kt)
- Refactor to fetch and render the live policy from the server using a `WebView`, identical to the Terms screen.

---

## Verification Plan

### Manual Verification
1.  **Post-Signup Acceptance:** Complete a signup and OTP verification. Verify the Terms screen appears with the updated "T&C and Privacy Policy" checkbox.
2.  **Permanent Access:** Go to the Account screen and verify the legal documents open correctly from the menu.
3.  **FAQ Population:** Open the Help Center. Verify the Navigation, Earnings, and Wallet categories are populated with professional answers.
4.  **Policy Accuracy:** Confirm the 25% and 75% figures are clearly mentioned in the FAQs and legal text.
