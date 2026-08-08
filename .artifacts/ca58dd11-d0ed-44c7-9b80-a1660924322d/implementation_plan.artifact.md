# Implementation Plan - Backend Stability & Crash Prevention

Diagnose and resolve the "502 Bad Gateway" issue by implementing robust error handling, boot-time safety checks, and a comprehensive system diagnostic script.

## User Review Required

> [!IMPORTANT]
> - **Potential Crash**: The 502 error indicates the backend is crashing or failing to start. This is often caused by invalid JSON in the `.env` (Firebase key) or missing database columns.
> - **Defensive Boot**: I will update the system to prevent a "Crash-on-Boot" even if external services (like Firebase or Email) are misconfigured.

## Proposed Changes

### 1. Boot Safety (Backend Services)

#### [MODIFY] [fcmService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend/src/services/fcmService.js)
- Wrap `JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT)` in a try-catch block.
- Log a warning instead of crashing if the key is invalid.

#### [MODIFY] [emailService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend/src/services/emailService.js)
- Add safety checks to ensure the SMTP transporter is only initialized if valid credentials exist.

---

### 2. Logic Robustness

#### [MODIFY] [authController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend/src/controllers/authController.js)
- Enhance error logging for the signup transaction.
- Ensure the `client.release()` is always called, even on early returns.

---

### 3. System Diagnostic Tool

#### [NEW] `backend/scratch/check_system.js`
- A script to run on your VPS that checks:
    - **Database**: Verifies connection and existence of the `role` column.
    - **Environment**: Verifies that JWT and SMTP keys are loaded.
    - **Permissions**: Verifies the `uploads` directory exists and is writable.

---

## Verification Plan

### Manual Verification (Run on VPS)
1.  **System Check**: Run `node backend/scratch/check_system.js`. This will point out exactly what is missing or broken.
2.  **Log Monitor**: Run `pm2 logs pikop-api --lines 50` while attempting a signup.
3.  **App Test**: Verify that if the Email or Firebase keys are broken, the user can still sign up (the email would fail, but the app wouldn't return 502).
