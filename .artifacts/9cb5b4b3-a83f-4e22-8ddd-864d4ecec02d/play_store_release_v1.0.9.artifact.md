# 📦 Google Play Store Release Build: Pikop v1.0.9 (Version Code 12)

**Date**: September 27, 2026
**App Version**: `v1.0.9`
**Version Code**: `12`
**Target SDK**: `36` (Android 15)
**Package Name**: `com.ng.pikop`

---

## 🎯 What's New in Version 1.0.9 (Version Code 12)

1. **Redesigned Role Selection Screen ("How do you want to use Pikop?")**:
   - Upgraded with transparent 3D PNG icon assets (`role_icon_send.png`, `role_icon_earn.png`, `role_icon_sell.png`, `role_icon_fleet_partner.png`, `role_icon_business_account.png`).
   - Color-glow borders & subtle background tints matching each role's theme color palette.
   - Circular arrow-forward affordances in the bottom-right corner of each card.
   - Organizational grouping divider: `"FOR ORGANIZATIONS"` separating personal and corporate accounts.
   - Restyled login link ("Log in →") and trust signal line ("Your data is safe with us" + shield icon).

2. **Real-Road Live Tracking Map Route, Solid Green Lines & Marker Travel**:
   - Replaced fake L-shaped curve generator with real-road network routing using Google Maps Directions API & OSRM routing engine.
   - Renders live tracking and navigation routes as **solid, vibrant brand green lines** (`#00E676`, `width = 14f`, `JointType.ROUND`, `RoundCap()`).
   - Smoothly animates the agent vehicle marker traveling along actual street turns and roundabouts.

3. **Web Admin Dashboard & Guest Real-Road Tracking**:
   - Upgraded `/admin/orders/:id/track` and public guest tracking views (`/guest/:orderId`) with OSRM real-road GeoJSON street network polylines in solid brand green (`#00E676`).

4. **UI Contrast & Text Visibility Enhancements**:
   - Refactored Customer Home Screen grid buttons (**My Wallet**, **Saved Places**, **Support Hub**, **Settings**) to use `surfaceVariant` containers with high-contrast text, eliminating white-on-white text visibility issues in dark theme.
   - Enhanced secondary labels and address texts across Order Summary breakdown (`OrderQuoteScreen.kt`) and Offer cards (`IncomingOfferComponent.kt`).

5. **Checkout & Mission Creation Reliability**:
   - Fixed mission creation error on promo code application (`TESTER100`) by supporting both UUIDs and promo code strings safely in backend coupon queries.
   - Clamped logistics subtotal in Order Summary breakdown (`maxOf(0.0, deliveryFee - discount)`), eliminating negative subtotal rendering (`₦0.0` for 100% free tester promos).

---

## 🚀 Release Artifact File Paths

> [!IMPORTANT]
> Upload `app-release.aab` (`versionCode = 12`) to your Google Play Console under **Production** or **Testing** track.

- **Google Play App Bundle (.aab)**:
  `C:\Users\MOSES\AndroidStudioProjects\Pikop\app\build\outputs\bundle\release\app-release.aab`

- **Signed Release APK (.apk)**:
  `C:\Users\MOSES\AndroidStudioProjects\Pikop\app\build\outputs\apk\release\app-release.apk`

- **R8 / Proguard De-obfuscation Mapping File**:
  `C:\Users\MOSES\AndroidStudioProjects\Pikop\app\build\outputs\mapping\release\mapping.txt`
