# Implementation Plan - SMS Service Diagnostic & Hardening

This plan addresses the non-functional SMS service by implementing robust error handling, flexible channel selection, and verified request patterns for the Termii API.

## User Review Required

> [!IMPORTANT]
> **Sender ID Verification:** If your custom Sender ID "Pikop" has not yet been fully approved by Termii for the DND route, messages will fail.
>
> **Action Requested:** Please confirm if "Pikop" is an approved Sender ID on your Termii dashboard. If not, we should use the default **"N-Alert"** for maximum reliability during testing.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [smsService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/smsService.js)
- **Robust Config:** Added `.trim()` to API keys and secrets to prevent "Invalid Key" errors caused by hidden spaces.
- **Header Hardening:** Added explicit `Content-Type: application/json` and `Accept: application/json` to all Termii requests.
- **Resilient Channel Logic:**
    - Default to `dnd` channel but provide a fallback or allow it to be configured via `.env`.
    - Added a `SENDER_ID` fallback to `"N-Alert"` if the custom ID fails or is not provided.
- **Transparent Logging:**
    - Updated the `catch` block to log the **entire Termii error response body**. This will reveal exactly why a message was rejected (e.g., "Invalid API Key", "Insufficient Balance", "Sender ID not found").
- **OTP Endpoint Alignment:** Verified and ensured the use of the `/sms/otp/send` and `/sms/send` endpoints.

#### [MODIFY] [authController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/authController.js)
- **Signup visibility:** Added a console log to the signup flow to indicate if the initial SMS trigger succeeded or failed.

---

## Verification Plan

### Automated Tests
- Syntax check backend: `node -c ...`.

### Manual Verification
1.  **Direct Log Audit:**
    - Trigger a signup in the app.
    - Check the VPS logs: `pm2 logs pikop-v3`.
    - **Expected:** You will see a detailed log entry from Termii. If it fails, the log will now contain the specific reason code from their API.
2.  **Generic Sender Test:**
    - Temporarily set `TERMII_SENDER_ID` to `"N-Alert"` in `.env`.
    - Test delivery again. This isolates whether the issue is with the "Pikop" Sender ID approval.
3.  **Balance Check:**
    - If the logs show "Insufficient Balance," please top up your Termii account.
