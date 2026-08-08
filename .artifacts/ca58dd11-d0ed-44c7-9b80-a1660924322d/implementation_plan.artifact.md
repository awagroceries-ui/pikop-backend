# Implementation Plan - Didit KYC Integration

Replace the manual identity verification (ID/Selfie) with **Didit's hosted KYC verification** for Fulfillers, while maintaining manual uploads for vehicle-specific documents.

## User Review Required

> [!IMPORTANT]
> - **Didit Configuration**: You will need to add `DIDIT_API_KEY` and `DIDIT_WEBHOOK_SECRET` to your VPS `.env` file.
> - **Workflow ID**: I will use the "Free KYC" workflow ID (`f718c93d-e9d2-432b-b964-7ebf702eceb8`) provided in the instructions.
> - **Unified Status**: A Fulfiller's `kyc_status` will only become `VERIFIED` once **both** the Didit identity check is "Approved" **and** manual vehicle documents are approved by an Admin.

## Proposed Changes

### 1. Database & Schema (Backend)

#### [NEW] `backend/migrations/1722980000000_didit_kyc.js`
- **Fulfillers**: Add `didit_session_id` (varchar), `didit_verification_status` (enum: not_started, pending, approved, declined, needs_review), and `didit_verified_at` (timestamp).
- Default `didit_verification_status` to `not_started`.

---

### 2. Backend Logic (Didit Integration)

#### [NEW] `backend/src/services/diditService.js`
- `createSession(userId)`: Calls `POST /v3/session/` with the "Free KYC" workflow ID and `vendor_data` (userId).
- `verifyWebhook(headers, body)`: Implements the required canonicalization and HMAC-SHA256 signature verification for Didit webhooks.

#### [MODIFY] `backend/src/controllers/fulfillerController.js`
- `startDiditVerification`: New endpoint to initialize the Didit session and return the `session_token` and `url`.
- `handleDiditWebhook`: Handles `Approved`, `Declined`, and `In Review` events from Didit. Updates the `fulfiller` record and checks if the overall `kyc_status` can be promoted.

#### [MODIFY] `backend/src/routes/fulfillerRoutes.js`
- Register the new `start-verification` and `webhook` routes.

---

### 3. Android: Fulfiller Onboarding (UI & SDK)

#### [MODIFY] `build.gradle.kts` (Project & App)
- Add Didit's custom Maven repository.
- Add `me.didit:didit-sdk` dependency.

#### [MODIFY] `ApiService.kt`
- Add `startDiditVerification` endpoint returning `{ url, session_token, session_id }`.

#### [MODIFY] `KycUploadScreen.kt`
- **Identity Step**: Replace the "ID Card" image picker with a "Verify Identity" button.
- **Didit SDK Integration**: When clicked, call the API, then launch `DiditSdk.startVerification(sessionToken)`.
- **Progress Handling**: Show a "Verification Pending" state while waiting for the webhook to update the status.
- **Vehicle Docs**: Keep the existing manual upload UI for License, Insurance, etc.

---

### 4. Admin Command Center

#### [MODIFY] `adminController.js` & `kyc_queue.ejs`
- Update the KYC queue to display the Didit Verification Status.
- Add a "View on Didit" link for `needs_review` statuses.
- Prevent final "Verify Fulfiller" action until Didit is "Approved".

## Verification Plan

### Manual Verification
1.  **Start Flow**: Tap "Verify Identity" in the Fulfiller app and verify the Didit hosted flow opens correctly.
2.  **Webhook Simulation**: Use a tool like Postman to simulate a signed Didit "Approved" webhook and verify the database updates.
3.  **Admin Review**: Verify that the Admin can see the Didit status and only approve the fulfiller if identity is clear.
4.  **Auto-Promotion**: Upload vehicle docs, get them approved, then complete Didit; verify `kyc_status` moves to `VERIFIED`.
