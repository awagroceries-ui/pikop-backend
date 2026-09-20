# Implementation Plan - Final Comprehensive Audit & Hardening

This plan covers the final set of refinements and security hardening required before the Pikop platform is ready for Playstore and production use.

## Proposed Changes

### 1. Security & Privacy Hardening (Backend)

#### [MODIFY] [adminController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/adminController.js)
- Remove sensitive login logs that include `hash_preview` and the result of the `match` boolean.
- Use `console.info` or specialized logging levels for non-critical flow tracing.

#### [MODIFY] [fulfillerController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/fulfillerController.js) & [authController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/authController.js)
- Redact PII (Personal Identifiable Information) from console logs in the KYC and Signup flows. Never log full request bodies that contain Passports, DOBs, or plain passwords.

---

### 2. Operational Stability (Backend)

#### [REFACTOR] Multi-row Database Transactions
Ensure the following flows are fully atomic using `BEGIN/COMMIT`:
- **`updateKYCStatus`**: Updates both `fulfillers` and `users` roles.
- **`processMissionSettlement`**: (Confirmed already using transactions, will do a final sanity check).

#### [MODIFY] [scheduledOrderJob.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/jobs/scheduledOrderJob.js)
- Wrap the job execution in a try-catch to ensure one failed activation doesn't kill the entire interval loop.

---

### 3. Android App: Final Polish & UX

#### [REFACTOR] Cleanup Noise
- Resolve the 15+ "Unused Parameter/Variable" warnings identified in `MainActivity.kt`, `FulfillerDashboardScreen.kt`, and `SupportHubScreen.kt` to ensure clean, maintainable code.

#### [MODIFY] [TrackOrderScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/TrackOrderScreen.kt) & [ActiveOrderScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/ActiveOrderScreen.kt)
- Add a "Tap to Copy" feature for the **Order ID**. Users and Agents often need this when contacting support.

#### [MODIFY] [CommerceCheckoutScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/commerce/CommerceCheckoutScreen.kt)
- Add a "Clear Cart" warning dialog if a user tries to add an item from a different merchant while their cart is non-empty.

---

## User Review Required

> [!IMPORTANT]
> **Production Logging**
> In production, `console.log` entries are often captured by log management tools. Redacting PII now is a critical step for NDPA (Nigeria Data Protection Act) compliance.

> [!NOTE]
> **Order ID Visibility**
> Making the Order ID easy to copy reduces friction for your support team significantly.

---

## Verification Plan

### Manual Verification
1.  **Security Audit**: Verify that logs on the VPS no longer show hashes or full KYC payloads.
2.  **UX Polish**: Verify the "Order ID" is copyable.
3.  **Cart Integrity**: Try to add a "Kitchen" meal to a cart containing "Groceries" items. Confirm the "Clear Cart" prompt appears correctly.
4.  **Dark Mode Sanity**: Perform one last check of the `PaymentConfirmationScreen` in Dark Mode to ensure the "Success" state is visible.
