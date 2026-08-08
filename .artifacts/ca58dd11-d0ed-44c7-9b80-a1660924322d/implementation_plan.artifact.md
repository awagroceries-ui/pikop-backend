# Implementation Plan - Fix Didit Session Creation

Resolve the "failed to start verification" error by ensuring the backend correctly loads environment variables and providing more detailed error logging.

## User Review Required

> [!IMPORTANT]
> - **Environment Path**: The `diditService.js` was using a default `dotenv.config()` which may fail depending on the execution context. I will standardize it to point to the root `.env` file.
> - **API Key Check**: I will add a safety check to ensure the `DIDIT_API_KEY` is present before making the request, returning a clear error if it's missing.

## Proposed Changes

### 1. Backend: Service Reliability

#### [MODIFY] [diditService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend/src/services/diditService.js)
- Standardize `.env` loading using an absolute path to the backend root.
- Add explicit logging of the `workflow_id` and presence of the `API_KEY` (masked) to help debug on the VPS.

#### [MODIFY] [fulfillerController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend/src/controllers/fulfillerController.js)
- Improve error handling in `startDiditVerification` to return more specific error messages from the Didit API if available.

---

### 2. Android: UI Feedback

#### [MODIFY] [KycUploadScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/KycUploadScreen.kt)
- Update the Toast message to show the actual error message from the server (if available) to help identify the root cause during testing.

---

## Verification Plan

### Manual Verification (Run on VPS)
1.  **Code Update**: Apply the changes and restart the API.
2.  **Log Check**: Run `pm2 logs pikop-api` and look for the new "Didit Service Initialized" log.
3.  **App Test**: Tap the "Identity Verification" button and verify the Didit flow starts or provides a specific error (e.g., "Invalid API Key").
