# Implementation Plan - Success Celebration Animation

This plan introduces a reusable, brand-colored success celebration animation triggered during key milestones across the app.

## Proposed Changes

### 🎨 1. Reusable Animation Component

#### [NEW] [SuccessCelebrationOverlay](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/ui/components/SuccessCelebrationOverlay.kt)
- **Visuals**: A smooth, single-burst particle effect using the Pikop brand palette (`PikopGreen`, `PikopLemonGreen`, `PikopGold`, `PikopOrange`).
- **Safety**: No strobing or rapid flickering. A single scale and fade-out transition.
- **Accessibility**:
    - Automatically detects system "Reduce Motion" settings.
    - Fallback: Shows a static, high-contrast success checkmark icon with a gentle fade-in, omitting the particle burst.
- **Auto-dismiss**: Animation automatically clears after 2 seconds or on a screen tap.

#### [NEW] [CelebrationViewModel](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/ui/components/CelebrationViewModel.kt)
- A simple, singleton-scoped ViewModel to trigger the celebration state globally.

---

### 🚀 2. Integration into Milestone Moments

#### [MODIFY] [MainActivity.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/MainActivity.kt)
- Add `SuccessCelebrationOverlay` as a top-level component above the `NavHost` to ensure it can overlay any screen.

#### [MODIFY] [OrderQuoteScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/OrderQuoteScreen.kt)
- Trigger celebration on successful mission activation (standard and scheduled).

#### [MODIFY] [CommerceCheckoutScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/commerce/CommerceCheckoutScreen.kt)
- Trigger celebration on successful order placement.

#### [MODIFY] [AddEditProductScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/merchant/AddEditProductScreen.kt)
- Trigger celebration when a new product is successfully saved.

#### [MODIFY] [TrackOrderScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/TrackOrderScreen.kt)
- Trigger celebration when a Customer's order moves to `DELIVERED` or `RELEASED`.

---

## User Review Required

> [!IMPORTANT]
> **Trigger Frequency**
> As per instructions, this animation will ONLY trigger for the first time a user completes these actions or for specific milestones (e.g., first order, first product). Routine repeated actions will continue to use standard toasts or snackbars.

## Verification Plan

### Manual Verification
1.  **Visual Polish**: Verify the animation uses all four brand colors and is a single smooth burst.
2.  **Accessibility**:
    - Turn on "Remove animations" in Android developer settings.
    - Verify only the static checkmark appears.
3.  **Non-Blocking**: Tap the screen while the animation is playing to confirm it dismisses and allows interaction with the UI below.
4.  **Milestone Test**: Create a new product. Verify the celebration plays. Edit an existing product. Verify NO celebration plays (routine action).
