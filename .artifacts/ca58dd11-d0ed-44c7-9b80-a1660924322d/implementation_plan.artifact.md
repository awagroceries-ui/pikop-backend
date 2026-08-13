# Implementation Plan - Fix KYC Verification Interruption

The identity verification process stops after the first capture attempt. This is likely due to process death on the 1GB RAM device or a loss of session state when returning from the camera activity.

## User Review Required

> [!IMPORTANT]
> I will be changing how the verification session is handled to ensure it persists across process death. I will also add more detailed logging to help diagnose why the SDK UI is closing prematurely.

## Proposed Changes

### [App] (file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app)

#### [MODIFY] [KycUploadScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/KycUploadScreen.kt)
- **Persist Session Data**: Use `rememberSaveable` to store the `session_token` and `session_id`. This prevents the app from creating a new verification session every time the user returns to the screen after a process death.
- **Improve Error Feedback**: Add Toasts and Logs within the SDK callback to see if it is being triggered when the UI closes.
- **Session Reuse**: Only call `api.startDiditVerification()` if we don't already have a valid session token in memory.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/MainActivity.kt)
- Add lifecycle logging to confirm if the activity is being destroyed during the capture process.

## Verification Plan

### Automated Tests
- Watch Logcat for `PikopKyc` and `PikopLifecycle` tags during the verification flow.

### Manual Verification
- Deploy to the device.
- Start the identity verification.
- Perform the first capture.
- Observe if the app returns to the KYC screen and if the session token is preserved.
- Check if the "Processing" or "Needs Review" status appears instead of "Not Started".
