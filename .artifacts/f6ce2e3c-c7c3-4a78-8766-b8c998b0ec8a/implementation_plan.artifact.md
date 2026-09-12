# Implementation Plan - Comprehensive Legal Framework

This plan implements a standard, comprehensive legal framework for Pikop, including a strong "Hold Harmless" clause to protect the company and its affiliates.

## User Review Required

> [!IMPORTANT]
> **Legal Protection:** I have added a robust "Indemnification and Limitation of Liability" section. This clause protects Awa Foods & Groceries (Pikop) from legal claims arising from the actions of independent fulfillers, user-provided content, or items being transported.
>
> **Dispute Resolution:** Standardized the governing law to the Federal Republic of Nigeria, with any legal proceedings directed to the courts of **Rivers State (Port Harcourt)**.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [legalController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/legalController.js)
- **`getTerms` & `getLegalConfig`**: Overhaul the HTML content with the following sections:
    - **1. User Obligations:** Accuracy of info, account security.
    - **2. Prohibited Items:** Drugs, hazardous materials, illegal substances, high-value currency.
    - **3. Financials:** Detailed breakdown of the 25% cancellation fee, 75% return fee, and non-refundable absence policy.
    - **4. Indemnification (Hold Harmless):** User agrees to indemnify Pikop against all losses, damages, and legal fees.
    - **5. Limitation of Liability:** Pikop is not responsible for losses caused by third-party agents or transit delays.
- **Privacy Policy**: Expand on data categories (KYC, GPS, financial references) and NDPR compliance rights.

---

### Android App
- No changes needed to the app code. The app is already configured to fetch this content live. The new comprehensive text will automatically appear in the `TermsScreen` and `PrivacyPolicyScreen`.

---

## Verification Plan

### Manual Verification
1.  **Content Audit:** Open the app and read through the new Terms. Verify the **Hold Harmless** clause and the **Port Harcourt** jurisdiction are clearly visible.
2.  **Formatting:** Ensure the long-form content is scrollable and readable in the app's `WebView`.
3.  **Sync:** Verify that the "Web" version of the terms (accessed via browser) matches the "App" version exactly.
