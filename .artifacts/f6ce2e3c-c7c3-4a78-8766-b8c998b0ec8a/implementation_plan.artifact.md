# Implementation Plan - Prepare for Google Play Store (Beta Track)

This plan outlines the technical steps to prepare the Pikop Android app for submission to the Google Play Store for closed testing.

## User Review Required

> [!IMPORTANT]
> **Production Signing Key:** To submit to the Play Store, you must generate a **Release Keystore**. I can help you with the Gradle configuration, but you will need to store this file securely and keep the password safe.
>
> **API Key Restrictions:**
> 1. Ensure your **Google Maps API Key** is restricted to the package name `com.ng.pikop` and your signing certificate's SHA-1 fingerprint in the Google Cloud Console.
> 2. Ensure your **Paystack Public Key** is the correct "Live" key (though for Beta testing, you may want to stay on the "Test" key until you are ready for real money).
>
> **Privacy Policy:** Google requires a public URL for your Privacy Policy. You should host the content of `PrivacyPolicyScreen.kt` on your website (e.g., `https://pikop.com.ng/privacy`).

## Proposed Changes

### Android App (`/app`)

#### [MODIFY] [build.gradle.kts](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/build.gradle.kts)
- Update `versionCode` (e.g., to `2`) and `versionName` (e.g., to `"1.0.1-beta"`) to distinguish it from the initial build.
- Enable **R8 Obfuscation** for the release build: set `isMinifyEnabled = true` and `isShrinkResources = true`.
- (Optional) Prepare a `signingConfigs` block so the terminal can build a signed AAB directly.

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/AndroidManifest.xml)
- Verify that `android:debuggable="false"` (automatically handled by release builds) and ensure the `label` and `icon` are final.

---

### Google Play Console Checklist

1.  **Internal/Closed Testing Track:** Create a new "Closed Testing" track and add the email addresses of your beta testers.
2.  **App Content:** Complete the "App Content" section (target audience, data safety, etc.).
3.  **Store Listing:** Upload high-resolution icons (512x512), a feature graphic (1024x500), and at least two phone screenshots.

## Verification Plan

### Automated Tests
- Run `./gradlew bundleRelease` to generate the **Android App Bundle (.aab)**.
- Verify the `.aab` file exists in `app/build/outputs/bundle/release/`.

### Manual Verification
- Install the generated AAB on a test device using `bundletool` or by uploading it to the **Internal Sharing** track in the Play Console.
- Confirm the app launches and API calls (Maps, Backend) still work with obfuscation enabled.
