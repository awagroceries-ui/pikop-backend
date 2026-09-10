# Walkthrough - Full KYC Visibility & Document Uploads

I have resolved the "partial visibility" issue on the Admin Dashboard and added the missing document upload capabilities to the fulfiller onboarding flow.

## Changes Made

### 1. Enhanced Admin KYC Review
- **Identity Visuals:** The dashboard now extracts and displays the **captured ID image and selfie** directly from the Prembly verification report. No more digging through raw JSON to see the user's identification.
- **Vehicle Audit Section:** Added a new dedicated section to the fulfiller review page that displays the vehicle's **Make, Model, Color, and Plate Number**.
- **Rich Document Previews:** Updated the "Operational Documents" section to show **image previews** for all uploaded files (NIN, License, etc.) instead of just text links.

### 2. New "Operational Documents" Step (Android)
- **The Gap:** The Android app was previously only capturing a profile photo, leaving the "Operational Documents" section empty for many users.
- **The Fix:** Inserted a new step in the onboarding flow that allows fulfillers to upload clear photos of their:
    - **Government ID** (NIN/Voter Card)
    - **Driver's License** (for Riders/Drivers)
    - **Vehicle Registration** (for Drivers)
- **Seamless Integration:** These uploads are sent directly to the backend's `kyc_documents` table and appear instantly for admin review.

### 3. Hardened Onboarding Logic
- **Step Synchronization:** Refactored the `LaunchedEffect` in `KycUploadScreen.kt` to handle the new 8-step flow (0-7).
- **Auto-Advance:** Ensure users are guided logically through Personal details -> Class selection -> Photo -> Identity scan -> Vehicle details -> Document uploads -> Bank details -> Submission.

## Verification Results

### Automated Build
- Ran `./gradlew assembleDebug`.
- **Result:** `BUILD SUCCESSFUL`.

### Deployment Instructions (For User)
Please apply these dashboard and API updates to your **VPS**:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
pm2 restart pikop-v3
```

## 📋 Testing the Fix
1. **Android Upload:** Open the fulfiller activation flow. You will reach the new "Operational Documents" step. Upload a test ID card image.
2. **Admin Verification:** Log in to the Admin Dashboard and review the fulfiller.
    - Confirm the **Identity Visuals** show the Prembly-captured images.
    - Confirm the **Vehicle Audit** shows the correct car/bike info.
    - Confirm the **Operational Documents** show a thumbnail of the ID card you just uploaded.
