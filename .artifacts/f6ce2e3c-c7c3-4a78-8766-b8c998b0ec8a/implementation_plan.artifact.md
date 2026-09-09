# Implementation Plan - Fix Account Buttons, Banking Fields & Activation Errors

This plan addresses non-functional buttons in the Account screen, unlocks banking details for editing, and fixes the "Duplicate Key" error in fulfiller activation.

## Problem Description
1.  **Duplicate Key Error:** Fulfiller activation fails because the cleanup query doesn't handle NULL `user_id` values, leaving conflicting email/phone records in the database.
2.  **Locked Banking Fields:** Banking details in `ProfileEditScreen.kt` feel "locked" or non-editable, possibly due to role mismatch or UI interaction issues.
3.  **Non-functional Buttons:** "Change Password" and "Delete Account" buttons in `AccountScreen.kt` are not responding or showing their respective dialogs.
4.  **Manual Date Entry:** The Date of Birth field still allows manual typing instead of forcing the calendar picker.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [fulfillerController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/fulfillerController.js)
- **Robust Cleanup:** Update the deletion query to handle `NULL` `user_id` values when clearing conflicting email/phone records:
    ```sql
    DELETE FROM fulfillers WHERE (email = $1 OR phone = $2) AND (user_id IS DISTINCT FROM $3)
    ```

---

### Android App

#### [MODIFY] [AccountScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/AccountScreen.kt)
- **Log Clicks:** Add `android.util.Log` and `Toast` messages to the `AccountOption` clicks to verify they are being triggered.
- **Surface Elevation:** Ensure `OutlinedCard` is properly interactive and not being "eaten" by the scrollable container.

#### [MODIFY] [ProfileEditScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/ProfileEditScreen.kt)
- **Case-Insensitive Role:** Update the role check to `role.uppercase() == "FULFILLER"`.
- **Unlock Fields:** Ensure `readOnly = false` for the account number and properly wire the bank dropdown.
- **Interaction Fix:** Wrap the bank fields in a way that works reliably within the scrollable column.

#### [MODIFY] [KycUploadScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/KycUploadScreen.kt)
- **Date Picker Polish:**
    - Fully disable the text field (`enabled = false`) and use a `Box` with `clickable` and `pointerInput` to ensure the calendar opens every time and the keyboard never appears.

---

## Verification Plan

### Automated Tests
- Syntax check backend: `node -c ...`.
- Build Android app: `./gradlew assembleDebug`.

### Manual Verification
1.  **Change Password / Delete:** Click these buttons in the Account screen. Verify that a dialog appears immediately.
2.  **Activation Fix:** Save fulfiller profile details. Verify no "Duplicate Key" or "Constraint" error occurs even if the email exists from an old session.
3.  **Bank Update:** As a fulfiller, successfully update the bank details in the profile.
4.  **Date Picker:** Tap the DOB field and verify the calendar appears instantly without the keyboard.
