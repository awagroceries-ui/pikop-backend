# Implementation Plan - Prioritize SMS OTP for Signups

This plan updates the signup verification flow to primarily use SMS OTP, using email as a secondary fallback if the SMS is not received.

## User Review Required

> [!IMPORTANT]
> **Orchestration Change:**
> - On signup, the system will now ONLY send an SMS verification code initially.
> - An email will still be sent after successful verification (Welcome email), but the OTP email is deferred until the user explicitly requests it as a fallback.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [authController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/authController.js)
- **`signup`**:
    - Remove the automatic `emailService.sendMail` for OTP.
    - Ensure SMS OTP via Termii is the primary trigger.
- **`requestEmailOtp` [NEW]**:
    - Add a new function to explicitly send the already-generated internal OTP to the user's email.
    - Implement a 60-second cooldown specifically for email requests.
- **`resendOtp`**:
    - Update to prioritize resending SMS, but perhaps provide an option for which channel to resend to.

#### [MODIFY] [authRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/authRoutes.js)
- Register `POST /api/v1/auth/request-email-otp`.

---

### Android App

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Add `requestEmailOtp(request: Map<String, String>): AuthResponse`.

#### [MODIFY] [EmailOtpScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/EmailOtpScreen.kt)
- **Rename & UI Pivot:**
    - Rename screen to `OtpVerificationScreen` (internal rename, keep route `email_otp` for stability or rename if safe).
    - Update header to **"Verify Your Account"**.
    - Primary instruction: *"Enter the 6-digit code sent to your phone [Phone]"*.
- **Add Fallback Action:**
    - Add a button or text link: **"Didn't get an SMS? Send to Email instead"**.
    - This link will trigger the new `requestEmailOtp` API.
- **Improved Feedback:** Show which channel the code was last sent to (Phone or Email).

---

## Verification Plan

### Automated Tests
- Syntax check backend: `node -c ...`.
- Build Android app: `./gradlew assembleDebug`.

### Manual Verification
1.  **Signup Flow:** Sign up with a valid phone. Verify that NO email OTP is received immediately, but the SMS arrives.
2.  **SMS Verification:** Enter the SMS code and verify the account activates.
3.  **Email Fallback Flow:**
    *   Wait for SMS. If it doesn't arrive, click "Send to Email instead".
    *   Verify the OTP email arrives.
    *   Enter the email code and verify it activates the account.
4.  **Cooldown:** Try to request email fallback multiple times quickly and verify the 60s cooldown works.
