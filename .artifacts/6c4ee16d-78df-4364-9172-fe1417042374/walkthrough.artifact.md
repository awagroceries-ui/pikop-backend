# Walkthrough - Fix Invisible FAQ Text

I have resolved the issue where FAQ question and answer text was appearing blank due to a lack of contrast against the app's white background.

## Changes Made

### 📱 Android Frontend (UI Fixes)
- **`FaqListScreen.kt`**: Updated the question list items to use `MaterialTheme.colorScheme.onBackground` instead of hardcoded white. This ensures titles are clearly visible as dark text.
- **`FaqDetailScreen.kt`**: Updated the article content text to use `MaterialTheme.colorScheme.onBackground`. The answer text is now fully visible and readable.
- **Brand Consistency**: Maintained the use of `PikopOrange` for article headers, which provides excellent contrast and brand alignment on the white background.

## Verification Results
- **Visual Visibility**: Confirmed that both questions in the list and full answers in the detail view are now rendered in a high-contrast dark color.
- **Android Build**: Successfully compiled with Gradle (`:app:assembleDebug`).
- **Data Integrity**: Confirmed the content itself was always present in the database/API; the issue was purely a visual color mismatch.

## Deployment Instructions
The fixes are in the latest Android build. Deploy the new APK to your device to see the changes:
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
