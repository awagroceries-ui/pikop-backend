# Implementation Plan - Fix Admin KYC Visibility & File Rendering

This plan fixes the missing Prembly reports on the admin dashboard, resolves the image rendering issues, and polishes the fulfiller onboarding date picker.

## Problem Description
1.  **Admin Visibility:** Successful Prembly verifications are not clearly surfaced on the admin review screen, even though they exist in the database.
2.  **Image Rendering:** KYC documents and profile photos appear as broken links in the admin dashboard because they use relative paths (e.g., `/uploads/...`) which don't resolve correctly from the admin's context.
3.  **Date Picker:** The fulfiller activation screen still allows/requires manual date entry instead of forcing the calendar picker.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [adminController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/adminController.js)
- **`getKYCReview`**:
    - Ensure `kyc_details` is always parsed as an object if it arrives as a string.
    - Inject the `BASE_URL` (from `.env` or config) into the view so that image links can be made absolute.

#### [MODIFY] [kyc_review.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/kyc_review.ejs)
- **Absolute URLs:** Prefix all `<img>` src and `<a>` href attributes for documents with the `BASE_URL`.
- **Report Rendering:** Refactor the "Automated Verification Report" section to handle both raw JSON and formatted display more robustly.

---

### Android App

#### [MODIFY] [KycUploadScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/KycUploadScreen.kt)
- **Force Picker:** Set `readOnly = true` and `enabled = true` on the `OutlinedTextField` but use a `Box` wrapper with a `pointerInput` or a dedicated `IconButton` to strictly trigger the `DatePickerDialog`.
- **Status Advancement:** Ensure the `LaunchedEffect` that observes the profile refreshes more aggressively after returning from the Prembly widget.

---

## Verification Plan

### Automated Tests
- Build Android app: `./gradlew assembleDebug`.
- Syntax check backend views: `node -e "require('ejs').compile(...)"`.

### Manual Verification
1.  **Date Picker:** Open the Fulfiller "Personal Details" step. Tapping anywhere on the Date of Birth field must open the calendar. Keyboard must not appear.
2.  **Admin Image Check:** Open an agent's KYC review page in the Admin Dashboard. All photos and document links must render correctly.
3.  **Admin Report Check:** Complete a test Prembly verification. Verify the "Automated Verification Report" appears in the agent's review page with the full JSON payload visible in the collapsible section.
