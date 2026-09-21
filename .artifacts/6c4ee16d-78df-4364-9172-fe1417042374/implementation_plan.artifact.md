# Implementation Plan - Bug Fixes & UX Polish

This plan addresses several critical issues identified across the Customer, Fulfiller, and Merchant modules, along with Play Store readiness.

## Proposed Changes

### 1. Customer Module: Missions Tab Fix
#### [MODIFY] [MainActivity.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/MainActivity.kt)
- Wire up the `onNewDelivery` callback for `OrdersDashboardScreen` to navigate to `"order_quote"`. This will fix the inactive "Send Something Now" and "+" buttons.

### 2. Pikop AI Agent: Connection Error
#### [MODIFY] [supportController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/supportController.js)
- Add explicit error logging for the Gemini AI service.
- Ensure the `GEMINI_API_KEY` is validated before attempting a connection.

### 3. Fulfiller Onboarding: UX & Form Fixes
#### [MODIFY] [KycUploadScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/KycUploadScreen.kt)
- **Date Picker**: Refactor the trigger to be more reliable by removing the transparent overlay and making the `OutlinedTextField` itself clickable.
- **Gender Selection**: Fix the `ExposedDropdownMenuBox` implementation to ensure the dropdown menu anchors correctly and is visible.
- **City/State Selector**: Add a two-stage dropdown for Nigeria States and their major cities (Lagos, Port Harcourt, Abuja, Kano, Ibadan, etc.) to replace the generic "Home Address" field for better data collection.

#### [MODIFY] [fulfillerController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/fulfillerController.js)
- Add detailed error logging in `submitApplication` to debug the 500 error reported by the user.

### 4. Merchant Verification: Null Constraint Fix
#### [MODIFY] [merchantController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/merchantController.js)
- Update `setupMerchantProfile` to fetch the authenticated user's email and include it as the `contact_email` in the `INSERT` statements for both `kitchens` and `vendors`. This resolves the "null value violates not-null constraint" error.

### 5. Play Store Release: Signed APK
#### [ACTION] Build Signed APK
- Execute the Gradle `assembleRelease` task to generate a signed APK in addition to the existing AAB. This is required for Play Console's initial package name verification in some regions.

---

## Verification Plan

### Automated Tests
- **Build Verification**: Run `./gradlew assembleRelease` to confirm both AAB and APK generation.

### Manual Verification
1.  **Missions Tab**: Tap "Send Something Now" and "+" buttons. Verify they open the Quote screen.
2.  **AI Agent**: Ask a question. Verify a response is received from Gemini.
3.  **Fulfiller Onboarding**:
    - Open Date Picker. Confirm it shows a calendar.
    - Open Gender dropdown. Confirm it shows options.
    - Select a State and then a City from the new dropdowns.
    - Submit the form and verify success (no 500 error).
4.  **Merchant Verification**: Submit the business setup form. Verify it saves correctly without a database error.
