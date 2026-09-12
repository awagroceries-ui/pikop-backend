# Implementation Plan - Standardized Professional Legal Framework

This plan overhauls the Pikop platform's Terms & Conditions and Privacy Policy to meet industry standards (e.g., Bolt Business), providing comprehensive legal protection and operational clarity.

## User Review Required

> [!IMPORTANT]
> **Commercial Agent Model:** We are adopting the "Commercial Agent" model. This means Awa Foods & Groceries acts as an intermediary technology platform. When a customer pays Pikop, the legal obligation to pay the independent Fulfiller is considered fulfilled.
>
> **Liability Limitations:** Following industry standards, Pikop's liability for any claim is capped at the total platform fees paid by the user in the **three months** preceding the claim.
>
> **Refunds as Credits:** To maintain platform stability, any approved refunds will be issued as **Pikop Wallet Credits** rather than cash reversals.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [legalController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/legalController.js)
Overhaul the `getTerms` and `getLegalConfig` content with the following professional structure:
1.  **Definitions:** Explicitly define Platform, User, Fulfiller (Independent Provider), and Service Fee.
2.  **The Pikop Service:** Define Pikop as a technology intermediary and commercial agent.
3.  **Payment & Billing:**
    - Breakdown of 75/25 split.
    - 25% Cancellation Fee (Matched, pre-pickup).
    - 75% Return Fee (Failed delivery).
    - Non-refundable Absence Policy.
4.  **Prohibited Items & Reporting:**
    - List of banned items (Drugs, weapons, etc.).
    - **Clause:** Discovery results in immediate reporting to authorities and disposal of items without compensation.
5.  **Liability & Indemnification:**
    - Robust "Hold Harmless" language.
    - Limitation of liability cap (3-month aggregate fees).
6.  **Confidentiality & Data Protection:** Aligned with NDPA/NDPR.
7.  **Governing Law:** Federal Republic of Nigeria. Jurisdiction: Port Harcourt, Rivers State.

#### [MODIFY] [emailService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/emailService.js)
- Update the `sendWelcomeEmail` policy summary to reflect this standardized language.

---

### Android App
- No changes required. The app already fetches and renders this content live from the server.

---

## Verification Plan

### Manual Verification
1.  **Legal Audit:** Open the Account menu in the app and read the "Terms & Conditions". Verify all sections (Liability, Prohibited Items, etc.) are present and formatted correctly.
2.  **Policy Accuracy:** Confirm the 25% cancellation and 75% return figures are correctly stated.
3.  **Email Check:** Trigger a signup and verify the welcome email contains the updated standardized policy summary.
4.  **Web Rendering:** Visit `https://api.pikop.com.ng/legal/terms` to ensure the web version matches the app version.
