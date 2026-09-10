# Implementation Plan - Fix Missing Admin KYC Data & Add Document Uploads

This plan addresses the missing verification report data and documents in the admin dashboard, and adds the missing document upload steps to the fulfiller onboarding flow.

## Problem Description
1.  **Admin Visibility Gap:** While the profile photo works, other captured data from Prembly (ID images, facial match) and vehicle details are not being displayed in the admin review screen.
2.  **Missing Android Steps:** The Android onboarding flow only captures a profile photo and vehicle strings, but lacks the ability to upload actual document files (ID card, Vehicle papers) into the `kyc_documents` table.
3.  **Data Fragmentation:** Verification reports are stored in a JSON blob (`kyc_details`) but only a few text fields are being extracted in the view.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [kyc_review.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/kyc_review.ejs)
- **Vehicle Audit:** Add a dedicated section to display vehicle `make`, `model`, `color`, and `registration_number`.
- **Identity Visuals:** Extract and display the captured ID image and selfie from the `kyc_details` JSON blob (mapping common fields from Prembly/IdentityPass like `data.image` or `data.face_image`).
- **Document Previews:** Update the operational documents section to show image previews for uploaded files rather than just a download link.

---

### Android App

#### [MODIFY] [KycUploadScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/KycUploadScreen.kt)
- **New Onboarding Step (Step 4):** Insert a new "Document Upload" step before the final submission.
- **Multi-Document Support:** Allow users to upload:
    - **Government ID** (NIN, Voter Card, etc.)
    - **Driver's License** (Conditional for Riders/Drivers)
    - **Vehicle Registration** (Conditional for Riders/Drivers)
- **File Picker Integration:** Use `ActivityResultContracts.GetContent` to allow selecting images or PDFs from the device gallery.
- **Backend Sync:** Wire these uploads to the `api/v1/fulfillers/kyc/document` endpoint.

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Add `uploadKycDocument` method:
    ```kotlin
    @Multipart
    @POST("api/v1/fulfillers/kyc/document")
    suspend fun uploadKycDocument(
        @Part("doc_type") type: RequestBody,
        @Part("expiry_date") expiry: RequestBody?,
        @Part file: MultipartBody.Part
    ): AuthResponse
    ```

---

## Verification Plan

### Automated Tests
- Build Android app: `./gradlew assembleDebug`.
- Syntax check backend views.

### Manual Verification
1.  **Android Onboarding:** Go through the flow as a Driver. Verify you can upload an ID card and Vehicle Registration.
2.  **Admin Dashboard:** Open the fulfiller's review page.
    *   Confirm the **Vehicle Section** shows the correct registration and model.
    *   Confirm the **Identity Section** shows the ID images from Prembly.
    *   Confirm the **Operational Documents** show clear previews of the uploaded ID and Vehicle papers.
