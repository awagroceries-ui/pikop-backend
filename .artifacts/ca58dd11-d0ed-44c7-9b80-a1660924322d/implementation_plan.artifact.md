# Implementation Plan - Advanced Features Bundle

Implement a suite of advanced features to make Pikop production-ready: Wallet UI, KYC Upload, Order Cancellations, and Push Notifications.

## User Review Required

> [!IMPORTANT]
> - **Firebase Setup**: You will need to create a project in the [Firebase Console](https://console.firebase.google.com/) and provide the `google-services.json` file for the Android app.
> - **File Storage**: For KYC uploads, I will store files in a `uploads/` directory on your VPS. In production, you might eventually want an S3-compatible bucket.
> - **Cancellation Fees**: I will implement a standard ₦200 cancellation fee if a driver has already been matched and has arrived/is nearby.

## Proposed Changes

### Phase 1: Wallet & Withdrawal UI (Android)
- **Backend**: Add `GET /api/v1/wallets/me` to fetch current balance and recent transactions.
- **Android**:
    - Create `WalletScreen.kt` with balance card and transaction list.
    - Implement "Request Payout" dialog for fulfillers.
    - Link to `OrdersDashboardScreen` and `FulfillerDashboardScreen`.

### Phase 2: Fulfiller KYC Upload (Full Stack)
- **Backend**:
    - Add `multer` for multipart/form-data handling.
    - Implement `POST /api/v1/fulfillers/kyc` to save document paths to the database.
- **Android**:
    - Create `KycUploadScreen.kt`.
    - Implement image selection and uploading logic for ID cards and licenses.

### Phase 3: Order Cancellations & Fees
- **Backend**:
    - Implement `POST /api/v1/orders/:id/cancel`.
    - Logic: If status is `MATCHED`, apply a cancellation fee to the user's wallet (or flag for next payment) and notify the fulfiller.
- **Android**:
    - Add "Cancel Order" buttons to `TrackOrderScreen` (User) and `ActiveOrderScreen` (Fulfiller).

### Phase 4: Push Notifications (FCM)
- **Infrastructure**:
    - Add `firebase-admin` to backend and Firebase SDK to Android.
- **Backend**:
    - Implement `POST /api/v1/auth/fcm-token` to store device tokens.
    - Trigger notifications for: "New Offer Nearby" (Fulfiller) and "Driver Arrived/Delivered" (User).
- **Android**:
    - Implement `PikopMessagingService` to handle incoming messages.

---

## Verification Plan

### Automated Tests
- Integration tests for wallet balance deductions during cancellation.
- API tests for file upload endpoints.

### Manual Verification
- Upload a test ID card and verify it appears in the Admin Dashboard KYC queue.
- Request a payout and verify a `PENDING` withdrawal record appears in the DB.
- Trigger a mock FCM message to the device.
