# Implementation Plan - Codes & Guest Communication Audit

This plan addresses gaps identified in the universal coverage of pickup/delivery codes and guest SMS communications across all mission types.

## 🔍 Audit Report

### 1. Pickup & Delivery Codes
- **Standalone Dispatch**: [WORKING] Codes generated and hashed correctly.
- **Marketplace (Prepaid)**: [WORKING] Codes generated in `activatePaidMission`.
- **Marketplace (COD)**: [WORKING] Codes generated in `initializeCommerceOrder`.
- **User-to-User**: [WORKING] Codes generated and correctly gated (only participants see them).

### 2. Guest SMS & Links
- **Guest Payer (COD)**: [WORKING] `sendSecurePaySms` triggered for guest receivers.
- **Guest Receiver (Non-COD)**: [BROKEN] No "Incoming Delivery" SMS is sent when a mission is first created for a guest. They only get an SMS *after* pickup.
- **Guest Payer (Sender-initiated)**: [BROKEN] If a sender initiates a Secure Pay for a guest, the guest receives the link, but the communication logic is scattered.

## Proposed Changes

### Backend (Node.js)

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- Implement `triggerInitialGuestCommunications`:
    - If `recipient_type === 'GUEST'`, send a "You have an incoming delivery" SMS immediately.
    - If `isSecurePay && !isPayerInitiator` (Receiver pays), send the "Secure Pay Request" SMS immediately.
- Call this helper in `createOrder`.

#### [MODIFY] [paymentController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/paymentController.js)
- Call `triggerInitialGuestCommunications` in `activatePaidMission` to handle cases where an app user pays for a delivery to a guest.

#### [MODIFY] [smsService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/smsService.js)
- Add `sendNewDeliveryAlert(phone, senderName, orderId)` template.

---

### Android Frontend (Compose)

#### [MODIFY] [CustomerHomeScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/CustomerHomeScreen.kt)
- Finalize "Settings" button link (was previously attempted but lacked `navController`). I will pass the `onNavigateToAccount` lambda.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/MainActivity.kt)
- Pass the account navigation to `CustomerHomeScreen`.

## Verification Plan

### Manual Verification
1.  **Guest Receiver Test**: Create a standard (non-COD) dispatch mission to a non-app phone number. Verify the guest receives an SMS *immediately* upon creation, not just at pickup.
2.  **Marketplace Guest Test**: Order a meal for a guest receiver (paid via card). Verify the guest receives a tracking link SMS once the order is activated.
3.  **Code Security**: Log in as User A. Try to view codes for Order B (owned by User C). Verify codes are `null` in the API response.
4.  **Settings Test**: Tap the Settings button on Home. Verify it opens the Account screen.
