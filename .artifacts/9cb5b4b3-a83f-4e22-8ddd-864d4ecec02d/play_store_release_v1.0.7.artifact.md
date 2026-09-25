# 📦 Google Play Store Release Build: Pikop v1.0.7 (Version Code 10)

**Date**: September 25, 2026
**App Version**: `v1.0.7`
**Version Code**: `10`
**Target SDK**: `36` (Android 15)
**Package Name**: `com.ng.pikop`

---

## 🎯 What's New in Version 1.0.7 (Version Code 10)

1. **Instant Mission Creation & Coupon Hardening**:
   - Fixed mission creation error on promo code application (`TESTER100`). Coupons are now queried flexibly by UUID or code string without database syntax errors.
   - Clamped logistics subtotal in Order Summary breakdown to prevent negative number rendering (`₦0.0` for 100% free tester promos).

2. **Instant Mission Dispatch & Real-Time Socket Sync**:
   - Refactored dispatch engine (`dispatchService.js`) to broadcast `new_mission_offer` to room `online_fulfillers` and targeted user rooms simultaneously.
   - Added real-time Socket.IO listeners on the Agent Dashboard (`FulfillerDashboardScreen.kt`) and a 5-second background polling loop for instant mission record updates.

3. **Live Agent GPS Location & Hotspot Map**:
   - Resolved camera centering on the agent's actual GPS location (`zoom 14f`) with a blue **"Your Location"** marker.
   - Fixed hotspot map overlay to render demand zones without forcing the camera away from the agent's location/city.

4. **Updated Platform Fees & Commissions**:
   - Cash on Delivery (COD) / Escrow Platform Fee set to **5%**.
   - Dispatch Commission set to **20%** (Agents keep **80%** of delivery fares).
   - Unified Marketplace Commission set to **5% across all categories** (Food, Groceries, Shop).

5. **Merchant & Agent Onboarding Pickers**:
   - Paystack bank dropdown selector & 10-digit account auto-resolution for Merchants.
   - Cascading Nigeria State and City dropdown pickers supporting 16+ Nigeria States.
   - Native Material 3 Date Picker for Date of Birth & Gender dropdown selector (`Male`, `Female`, `Other`).

---

## 🚀 Release Artifact File Paths

> [!IMPORTANT]
> Upload `app-release.aab` (`versionCode = 10`) to your Google Play Console under **Production** or **Testing** track.

- **Google Play App Bundle (.aab)**:
  `C:\Users\MOSES\AndroidStudioProjects\Pikop\app\build\outputs\bundle\release\app-release.aab`

- **Signed Release APK (.apk)**:
  `C:\Users\MOSES\AndroidStudioProjects\Pikop\app\build\outputs\apk\release\app-release.apk`

- **R8 / Proguard De-obfuscation Mapping File**:
  `C:\Users\MOSES\AndroidStudioProjects\Pikop\app\build\outputs\mapping\release\mapping.txt`
