# Milestone 22: Didit KYC Integration Task List [DONE]

## Phase 1: Database & Schema
- [x] Create `didit_kyc` migration (`didit_session_id`, `didit_verification_status`, `didit_verified_at`)

## Phase 2: Backend Implementation
- [x] Create `diditService.js` (Session creation & Webhook verification)
- [x] Add `start-verification` endpoint in `fulfillerController.js`
- [x] Implement `handleDiditWebhook` in `fulfillerController.js`
- [x] Register new routes in `fulfillerRoutes.js`

## Phase 3: Android App Integration
- [x] Add Didit Maven repo and dependency in `build.gradle.kts`
- [x] Update `ApiService.kt` with Didit endpoints
- [x] Refactor `KycUploadScreen.kt` with Didit SDK flow

## Phase 4: Admin Dashboard
- [x] Update KYC queue view with Didit status indicators
- [x] Add "View on Didit" deep-links for operators

## Verification
- [x] Test session creation from Android
- [x] Test webhook processing and status updates
- [x] Verify Fulfiller auto-promotion logic
