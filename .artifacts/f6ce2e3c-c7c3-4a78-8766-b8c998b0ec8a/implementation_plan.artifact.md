# Implementation Plan - VPS Stability & System Hardening

This plan addresses critical errors found in the VPS logs, including AI classification failures, database lock issues, and SMS delivery rejections.

## User Review Required

> [!CAUTION]
> **Termii Action Required:** The logs show the error: `'Country Inactive. Contact Administrator to activate country'`.
>
> **Why this happens:** Your Termii account has not been configured to send messages to Nigeria (+234) using the **DND channel**.
>
> **How to fix:**
> 1. Log in to your [Termii Dashboard](https://termii.com/).
> 2. Go to **Settings** or **Coverage**.
> 3. Ensure **Nigeria** is activated for all channels, especially the **DND** route.
> 4. Ensure your **Sender ID** (Pikop) is registered for the DND route.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [geminiService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/geminiService.js)
- **Model Update:** Update model identifiers to use `-latest` suffixes (e.g., `gemini-1.5-flash-latest`).
- **Compatibility:** Add `gemini-pro` as a final fallback model to ensure item classification never fails.

#### [MODIFY] [walletService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/walletService.js)
- **Fix SQL Error:** Resolve the `FOR UPDATE` join conflict by explicitly locking only the `orders` table: `FOR UPDATE OF o`. This fixes the crash during mission completion/escrow release.

#### [MODIFY] [smsService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/smsService.js)
- **Helpful Diagnostics:** If Termii returns "Country Inactive," log a very clear instruction in the VPS console pointing to the Termii Dashboard activation.

---

## Verification Plan

### Automated Tests
- Syntax check backend: `node -c ...`.

### Manual Verification
1.  **AI Classification:** Create a mission with the description "Large Fridge." Verify it is correctly classified as `LARGE` (check via Admin or logs).
2.  **Mission Completion:** Complete a Secure Pay mission. Verify the escrow release logic no longer crashes the server.
3.  **SMS Delivery:** Once you activate Nigeria in Termii, verify signup SMS arrives instantly.
