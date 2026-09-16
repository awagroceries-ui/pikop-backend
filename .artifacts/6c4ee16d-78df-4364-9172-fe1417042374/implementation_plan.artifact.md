# Implementation Plan - Fix Invisible FAQ Text

This plan resolves the issue where FAQ question and answer text is invisible because it is hardcoded to white on a white background.

## 🔍 Diagnostic Summary
- **Root Cause**: Both `FaqListScreen.kt` and `FaqDetailScreen.kt` have text colors hardcoded to `Color.White`. Since the app's background is also white (per the recent branding update), the text is present but invisible.
- **Affected Files**:
    - `FaqListScreen.kt` (List item titles)
    - `FaqDetailScreen.kt` (Article titles and content)

## Proposed Changes

### Android Frontend (Compose)

#### [MODIFY] [FaqListScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/FaqListScreen.kt)
- Update `FAQListItem` to use `MaterialTheme.colorScheme.onBackground` or `PikopNearBlack` instead of `Color.White`.

#### [MODIFY] [FaqDetailScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/FaqDetailScreen.kt)
- Update the content `Text` component to use `MaterialTheme.colorScheme.onBackground` instead of `Color.White`.
- Verify the title color (`PikopOrange`) has sufficient contrast (it should be fine on white).

## Verification Plan

### Automated/Code Verification
- Verify successful Gradle build.

### Manual Verification
1.  **FAQ List**: Open the FAQ list for any category. Confirm question titles are clearly visible and black/dark.
2.  **FAQ Detail**: Open an FAQ article. Confirm the answer text is clearly visible and readable.
3.  **Contrast Check**: Ensure the colors align with the new brand theme and provide high readability.
