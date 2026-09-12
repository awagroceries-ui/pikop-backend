# System Integrity Audit Report - Pikop V3

This report summarizes the functional status of all features and integrations, identifies integrity gaps, and reviews current refund/legal alignments.

## 1. Integration Status

| Integration | Provider | Status | Notes |
| :--- | :--- | :--- | :--- |
| **Payments** | Paystack | ✅ Active | Initialize, Webhook, and Guest Checkout are operational. |
| **SMS/OTP** | Termii | ✅ Active | DND route with Generic fallback is operational. prioritized for signups. |
| **Identity** | Prembly | ✅ Active | ID image extraction and status polling implemented. |
| **Maps/Places**| Google | ✅ Active | Autocomplete bias to NG and real-time tracking are functional. |
| **AI Classifier**| Gemini | ✅ Active | Model identifiers updated to `-latest` for stability. |
| **Real-time** | Socket.io | ✅ Active | Location streams, status sync, and chat are functional. |

## 2. Refund & Cancellation Policies

### Current Implementation:
1.  **Recipient Absent Policy:**
    *   **Backend:** Strictly enforced. If marked `RECIPIENT_ABSENT`, the mission fare is settled (75/25) and no refund is issued to the sender.
    *   **Return Flow:** If a return is initiated, a new mission is created at **50% of the original fare**.
2.  **Cancellation Policy:**
    *   **App UI:** Claims ₦200 fee if matched.
    *   **Backend:** ❌ **Gap Found.** The backend `cancelOrder` currently just updates the status to `CANCELLED` without deducting a penalty fee from the wallet.

## 3. Legal Component Alignment

### Audit Findings:
- **Misalignment:** The content in `backend_v3/src/controllers/legalController.js` is significantly shorter and less detailed than the hardcoded text in `app/.../TermsScreen.kt`.
- **Inconsistency:** The App mentions a ₦200 cancellation fee, while the Backend Legal page does not explicitly mention it.
- **Sync Status:** ⚠️ **Partial.** The app displays legal pages from its own resources rather than fetching from the centralized `/api/v1/legal/terms` endpoint.

## 4. Functional Integrity Gaps (Refinements Needed)

### Logistics:
- **Fulfiller Action Gap:** The "Request Consent" (Leave at door) and "Failed Delivery" (10-min timeout) capabilities exist on the server but have **no corresponding buttons** in the Android `ActiveOrderScreen`.
- **Landmark UX:** Proximity-based landmark suggestions are active on the API, but the **autocomplete dropdown** in the Android `OrderQuoteScreen` needs better styling and triggering.

### Dynamic Pricing:
- **Weather Data:** The background job is active but currently defaults to "Clear". It needs a real connection to weather data to automate flood-prone surcharges.
- **Traffic Windows:** Initial corridors (e.g., Third Mainland Bridge) are seeded but require manual verification against live traffic patterns.

---

## 📋 Summary of Integrity
The core engine is **stable and functionally sound**. The most critical work remaining is **UI wiring** in the Fulfiller app for specialized logistics states (Consent/Failure) and **Centralizing Legal Text** to ensure the company is protected consistently across all platforms.
