# Implementation Plan - Ultra-Comprehensive Legal Framework Overhaul

This plan overhauls the Pikop platform's Terms & Conditions and Privacy Policy into a long-form, multi-section legal framework that matches the depth and rigor of industry leaders like Bolt and Uber.

## User Review Required

> [!IMPORTANT]
> **Legal Rigor:** This update will significantly increase the length and detail of your legal documents. It includes detailed sections on **Intellectual Property**, **Force Majeure**, **Severability**, and **Detailed User Conduct** rules.
>
> **Indemnification:** I am expanding the "Hold Harmless" clause into a dedicated, heavy-duty section to provide maximum protection for Awa Foods & Groceries.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [legalController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/legalController.js)
Overhaul `getTerms` and `getLegalConfig` with a 12-section standardized framework:
1.  **Definitions:** Precise legal terminology for all parties and fees.
2.  **User Eligibility & Account Security:** Responsibilities regarding credentials and authorized use.
3.  **Scope of Service:** Defining Pikop as a technology platform and Commercial Agent.
4.  **License & Intellectual Property:** Protection of the Pikop brand, code, and UI.
5.  **User Obligations & Conduct:** Prohibiting reverse engineering, scraping, or misuse.
6.  **Prohibited Items & Law Enforcement:** Expanded clause on illegal goods, reporting, and immediate disposal.
7.  **Financial Terms:** Explicit breakdown of 25% Cancellation, 75% Return, and non-refundable absence rules.
8.  **Liability & Warranty Disclaimer:** "As Is" service delivery and heavy liability limitations.
9.  **Indemnification (Hold Harmless):** Heavy-duty protection for the company and affiliates.
10. **Termination:** Conditions under which Pikop can suspend or delete accounts.
11. **General Provisions:** Severability, No-Waiver, and Force Majeure.
12. **Governing Law & Jurisdiction:** Federal Republic of Nigeria; Courts of Port Harcourt, Rivers State.

#### [MODIFY] [legalController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/legalController.js)
Overhaul the **Privacy Policy** into a 7-section framework:
1.  **Data Controllers:** Legal roles under the NDPA.
2.  **Information We Collect:** Categories including KYC, GPS, Device data, and Transactional logs.
3.  **Legal Basis for Processing:** Necessity for contract, legal obligation, and legitimate interest.
4.  **Data Sharing:** Detailed list of who sees what (Customer vs Fulfiller vs Admin).
5.  **Data Retention:** How long we keep data and the criteria for deletion.
6.  **Security Measures:** 2FA and encryption commitments.
7.  **Your Rights (NDPA):** Right to access, rectify, or erase data.

---

### Android App

#### [MODIFY] [TermsScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/TermsScreen.kt)
#### [MODIFY] [PrivacyPolicyScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/PrivacyPolicyScreen.kt)
- **Styling Hardening:** Update the `WebView` settings to ensure the text is properly padded and use a standard system font for maximum readability of the long-form text.

---

## Verification Plan

### Manual Verification
1.  **Document Scrutiny:** Open the app and verify the documents are now "long-form" with multiple numbered sections.
2.  **Navigation Check:** Ensure the `WebView` scrolls smoothly through the extended content.
3.  **Jurisdiction Audit:** Verify the "Port Harcourt, Rivers State" mention in section 12.
4.  **Law Enforcement Clause:** Confirm the "Report to Authorities" and "Disposal without Compensation" language is present and clear.
