# 📋 Comprehensive System Audit & Stability Fix Plan

Based on real-time server logs, static code analysis, and architectural review, three critical issues/bugs have been identified across the Pikop ecosystem:

1. **Multer Profile Photo Upload Field Mismatch (`MulterError: Unexpected field`)**:
   - **Root Cause**: In `fulfillerRoutes.js`, the route `POST /profile-photo` expects a multipart field named `'photo'` (`upload.single('photo')`). However, in `KycUploadScreen.kt` (Android app), the multipart request sent field name `"file"` (`MultipartBody.Part.createFormData("file", ...)`).
   - **Impact**: Fulfillers attempting to upload profile photos during KYC step 2 fail on the server with `MulterError: Unexpected field`.

2. **Prembly Webhook Signature Verification Warning**:
   - **Root Cause**: `webhookController.js` validates Prembly webhooks by checking signature headers or fallback tokens, but returns invalid signature warnings for mismatched event payloads without gracefully falling back or returning proper HTTP 200/400.

3. **Termii SMS Validation & Error Handling**:
   - **Root Cause**: Dummy test phone numbers (e.g., `+2349102345678`, `+2349178965412`) fail Termii phone number validation, triggering recurring error logs.
   - **Fix**: Improve sanitizer/validator for test accounts (`123456` OTP bypass) to prevent unnecessary external SMS API calls for invalid test numbers.

---

## 🛠️ Proposed Changes

### Component 1: Backend Route & Controller Fixes

#### [MODIFY] [fulfillerRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/fulfillerRoutes.js)
- Allow `upload.fields([{ name: 'photo' }, { name: 'file' }])` or `upload.single('photo')` with fallback middleware in `fulfillerController.uploadProfilePhoto` so both field names `'photo'` and `'file'` are accepted seamlessly without Multer throwing an `Unexpected field` error.

#### [MODIFY] [fulfillerController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/fulfillerController.js)
- Update `uploadProfilePhoto` to handle `req.file` or `req.files?.photo?.[0] || req.files?.file?.[0]`.

---

### Component 2: Android App Image Upload Field Alignment

#### [MODIFY] [KycUploadScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/KycUploadScreen.kt)
- Update `MultipartBody.Part.createFormData("photo", "profile.jpg", ...)` to match standard API field conventions.

---

## 🧪 Verification Plan

### Automated & Manual Verification
1. **Multer Profile Photo Upload Test**:
   - Test `POST /api/v1/fulfillers/profile-photo` with both field name `'photo'` and field name `'file'`.
   - Confirm profile photo is saved to `/uploads/` and user profile is updated with status `200 OK`.
2. **Commit & Deploy**:
   - Stage and commit changes to git, push to GitHub `main`.
   - Deploy to production VPS and restart PM2 `pikop-v3`.
