# Implementation Plan - Milestone 1: Platform Hardening

Establish production-grade security, dispatch logic, and operational baseline as specified in the Feature Build Prompts.

## User Review Required

> [!IMPORTANT]
> - **Security Window**: The `delivery_code` will no longer be visible in the database. It will only be generated and sent to the recipient the moment the Fulfiller marks the order as "Arrived at Delivery."
> - **GPS Locking**: I will update the Fulfiller app to automatically attach GPS coordinates to the delivery photo. Fulfillers will be flagged if they attempt to complete a delivery more than 200m from the target.
> - **Strict Eligibility**: Drivers (Trucks/Vans) will no longer see "Small" item offers (Agents and Riders only).

## Proposed Changes

### 1. Hardened Verification (Backend)

#### [NEW] `backend/migrations/1723000000000_hardened_baseline.js`
- **Orders**:
    - Rename `pickup_code` -> `pickup_code_hash`.
    - Rename `delivery_code` -> `delivery_code_hash`.
    - Add `capture_lat`, `capture_lng`, `capture_timestamp` for Proof-of-Delivery metadata.
    - Add `eligible_classes` (varchar array) to track who can see the order.

#### [MODIFY] `orderController.js`
- Use `crypto.randomInt` for secure code generation.
- Hash codes using `bcrypt` before storing.
- Trigger `delivery_code` generation only on the `ARRIVED_AT_DELIVERY` status transition.

---

### 2. Advanced Dispatch (Backend)

#### [MODIFY] `dispatchService.js`
- Update `findNearbyFulfillers` to filter by Fulfiller class (`agent`, `rider`, `driver`) matching the order's size tier.

#### [MODIFY] `authController.js`
- Add `resendOtp` endpoint with `RATE_LIMITED` status code and cooldown logic.

---

### 3. Proof-of-Delivery (Android)

#### [MODIFY] `ActiveOrderScreen.kt`
- **Hardened Capture**: Force camera-only photo capture (disable gallery access).
- **GPS Metadata**: Fetch and send high-accuracy GPS coordinates during the "Verify Delivery" step.

#### [MODIFY] `EmailOtpScreen.kt`
- Add "Resend Code" link with a visible 30-second countdown timer.

---

## Verification Plan

### Manual Verification
1.  **Code Security**: Try to use an old pickup code from a previous test; verify the system rejects it because it's now hashed.
2.  **Timing**: Create an order. Verify you *do not* receive the delivery code immediately. Mark the order as "Arrived at Destination" as a Fulfiller and verify the code is only sent then.
3.  **Eligibility**: Set a Fulfiller as "Driver" class. Create a "Small" order. Verify the offer never appears on the Driver's dashboard.
4.  **Resend**: Tap "Resend" on the OTP screen twice. Verify the second tap is blocked by the 30s timer.
