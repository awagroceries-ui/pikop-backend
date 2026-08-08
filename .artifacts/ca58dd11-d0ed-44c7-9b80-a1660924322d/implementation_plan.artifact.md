# Implementation Plan - Email Delivery Debug & Reliability

Diagnose and resolve the email delivery issue by adding verbose logging, SMTP connection verification, and robust path handling for environment variables.

## User Review Required

> [!IMPORTANT]
> - **SMTP Diagnostics**: I will add a script that tests your Brevo connection directly and prints the exact error (e.g., "Connection Timeout" or "Invalid Credentials").
> - **Path Standardization**: I will ensure all services look for the `.env` file in the same consistent location to prevent missing configuration bugs.

## Proposed Changes

### 1. Enhanced Diagnostics (Backend)

#### [NEW] `backend/scratch/test_email.js`
- A standalone script to test the SMTP transporter.
- It will attempt to send a test email and print the full error stack if it fails.
- This will reveal if port `587` is blocked by your VPS provider (TrueHost).

#### [MODIFY] `backend/src/services/emailService.js`
- Add `logger: true` and `debug: true` to the Nodemailer transporter (temporarily) to help identify handshaking issues.
- Standardize the `.env` loading path across all services.

---

### 2. Logic Robustness

#### [MODIFY] `backend/src/controllers/authController.js`
- Change `notificationService.sendOTPEmail` to a background promise with an error catcher, ensuring any failure to *start* the email process is caught and logged.

---

## Verification Plan

### Manual Verification (Run on VPS)
1.  **Test Script**: Run `node backend/scratch/test_email.js` and examine the output.
2.  **Logs**: Check `pm2 logs pikop-api` during a signup attempt to see the new verbose email logs.
3.  **Database Check**: Check the `notification_logs` table via SQL to see the `error_message` for failed attempts.
