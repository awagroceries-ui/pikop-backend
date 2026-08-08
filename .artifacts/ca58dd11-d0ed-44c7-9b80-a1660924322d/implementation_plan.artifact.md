# Implementation Plan - Transactional Email Notifications

Fix the OTP delivery issue and implement professional transactional emails for user welcoming and fulfiller approval, with a robust logging and duplicate-prevention system.

## User Review Required

> [!IMPORTANT]
> - **SMTP Configuration**: I noticed that the backend is currently *not* configured for email sending (it's marked as a `TODO`). I will need you to provide your SMTP details (Host, Port, User, Password) to be added to the `.env` file on your VPS.
> - **Email Templates**: I will implement high-quality, responsive HTML templates with inlined CSS (compatible with Gmail/Outlook/Apple Mail).
> - **Duplicate Prevention**: I will create a `notification_logs` table to track every email sent, ensuring no user receives the same transactional email twice.

## Proposed Changes

### 1. Database & Infrastructure (Backend)

#### [NEW] `backend/migrations/1722930000000_notification_logs.js`
- Create `notification_logs` table: `id`, `user_id`, `channel` (email), `template_name`, `sent_at`, `status` (success/failed).

#### [MODIFY] `backend/package.json`
- Add `nodemailer` as a dependency.

#### [MODIFY] `backend/.env`
- Add placeholders for `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASS`, and `EMAIL_FROM`.

---

### 2. Email Service & Templates (Backend)

#### [NEW] `backend/src/services/emailService.js`
- Initialize `nodemailer` transporter using environment variables.
- Implement `sendMail` with retry logic and error logging.

#### [NEW] `backend/src/services/notificationService.js`
- `sendOTPEmail(email, otp)`: Sends the 6-digit verification code.
- `sendWelcomeEmail(userId)`: Warm welcome for verified users.
- `sendFulfillerApprovedEmail(fulfillerId)`: Confirmation with "Go Online" instructions and payout reminders.
- Includes logic to check `notification_logs` before sending (guard against duplicates).

---

### 3. Logic Integration

#### [MODIFY] `backend/src/controllers/authController.js`
- **Signup**: Replace the `console.log` with a call to `notificationService.sendOTPEmail`.
- **Verify Email**: After successful verification, trigger `notificationService.sendWelcomeEmail` (asynchronous, won't block response).

#### [MODIFY] `backend/src/controllers/adminController.js`
- **KYC Approval**: After marking a fulfiller as verified, trigger `notificationService.sendFulfillerApprovedEmail`.

---

## Verification Plan

### Automated Tests
- **Integration Test**: Mock the email transporter and verify that `sendWelcomeEmail` is called exactly once after verification.
- **Log Verification**: Ensure a record is created in `notification_logs` for every successful send.

### Manual Verification
- Sign up with a real email and verify the OTP arrives.
- Verify the "Welcome" email arrives immediately after entering the correct OTP.
- Approve a test Fulfiller in the Admin Dashboard and verify they receive the detailed approval instructions.
