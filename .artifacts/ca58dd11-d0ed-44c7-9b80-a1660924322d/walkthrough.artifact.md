# Walkthrough - Fixing KYC Verification Interruption

I have implemented changes to ensure that the KYC identity verification process is robust against process death, which is common on 1GB RAM devices.

## Changes Made

### Android App

#### [MODIFY] [KycUploadScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/KycUploadScreen.kt)
- **Session Persistence**: Added `activeSessionToken` and `activeSessionId` using `rememberSaveable`. This ensures that even if the app is killed by the OS while the camera is open, it remembers the current verification session when it restarts.
- **Session Reuse Logic**: Modified the "Verify Identity" logic to reuse the existing `activeSessionToken` if available. This prevents multiple session requests to the backend, which could invalidate previous capture attempts.
- **Improved Callbacks**: Added detailed logging and a Toast message to the Didit SDK callback to confirm when the verification status is updated.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/MainActivity.kt)
- **Lifecycle Monitoring**: Added `onCreate` and `onDestroy` logging (including the Process ID). This allows you to see in Logcat if the app is actually restarting due to memory pressure during the capture flow.

## Verification Results

### Logcat Monitoring
- Filter by `PikopKyc` to see session token reuse:
  `D/PikopKyc: Resuming existing session: ...`
- Filter by `PikopLifecycle` to detect process restarts:
  `D/PikopLifecycle: MainActivity onCreate (Process ID: ...)`

### User Experience
- The app now asks for permissions immediately if they are missing.
- After a capture attempt, the app should return to the KYC screen and maintain the "Processing" or "Needs Review" status, rather than resetting to "Not Started".

> [!TIP]
> If you see the "MainActivity onCreate" log appear after a capture, it confirms the OS killed the app. Thanks to the `rememberSaveable` fix, your verification progress should now be preserved regardless.
