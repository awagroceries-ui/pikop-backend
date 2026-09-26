# 📋 Implementation Plan: Fix Invisible Text & High-Contrast Theme Adaptation

Fix invisible white-on-white text on the Customer Home Screen grid cards (`My Wallet`, `Saved Places`, `Support Hub`, `Settings`) and enhance text visibility across all theme cards.

---

## 🔍 Root Cause Analysis

As seen in the provided screenshot:
1. **White Text on Off-White Cards (`CustomerHomeScreen.kt`)**:
   - `ServiceButton` cards used a hardcoded light container color (`PikopGrey` `#F5F5F5`).
   - In dark theme, `Text(text = title)` defaulted to white text (`#FFFFFF`).
   - Rendering white text on an off-white `#F5F5F5` card produced **white-on-white invisible text** for "My Wallet", "Saved Places", "Support Hub", and "Settings".
   - Subtitle text used `PikopDarkGrey` (`#757575`), resulting in low contrast.

---

## 🛠️ Proposed Changes

### Component 1: Customer Home Screen (`CustomerHomeScreen.kt`)

#### [MODIFY] [CustomerHomeScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/CustomerHomeScreen.kt)
- Refactor `ServiceButton` composable:
  - Container color: `MaterialTheme.colorScheme.surfaceVariant` (adaptive dark slate in dark mode, light slate in light mode).
  - Icon tint: `MaterialTheme.colorScheme.primary` (`#008751` Pikop Green).
  - Title text: `MaterialTheme.colorScheme.onSurface` (bold crisp white in dark mode, bold dark slate in light mode).
  - Subtitle text: `MaterialTheme.colorScheme.onSurfaceVariant` (high-contrast secondary text).
- Update helpful tip card text color to `MaterialTheme.colorScheme.onSurfaceVariant`.

---

### Component 2: Order Summary & Offer Cards (`OrderQuoteScreen.kt` & `IncomingOfferComponent.kt`)

#### [MODIFY] [OrderQuoteScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/OrderQuoteScreen.kt)
- Update `SummaryLine` labels and `LocationInput` labels to `MaterialTheme.colorScheme.onSurfaceVariant` and `onSurface`.

#### [MODIFY] [IncomingOfferComponent.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/IncomingOfferComponent.kt)
- Update Pickup/Dropoff label titles and addresses to `MaterialTheme.colorScheme.onSurfaceVariant` and `onSurface`.

---

## 🧪 Verification Plan

### Automated & Manual Verification
1. Test Customer Home Screen (`CustomerHomeScreen.kt`) on connected **Samsung Galaxy S23 Ultra** (`192.168.1.2:42447`).
2. Verify "My Wallet", "Saved Places", "Support Hub", and "Settings" titles and subtotals/subtitles are 100% crisp, bold, and fully visible on screen (matching the user's screenshot).
3. Verify Order Summary breakdown (`OrderQuoteScreen.kt`) and Offer cards (`IncomingOfferComponent.kt`) render high-contrast text.
4. Rebuild debug APK (`gradle_build("app:assembleDebug")`) and deploy to device.
5. Stage, commit, and push changes to GitHub `main`.
