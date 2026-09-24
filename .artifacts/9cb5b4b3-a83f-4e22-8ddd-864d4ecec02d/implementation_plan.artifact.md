# 📋 Implementation Plan: Merchant Operating/Opening Hours Audit & Feature Implementation

Audit and implement complete Operating/Opening Hours management for Merchant accounts across the Pikop platform.

---

## 🔍 Audit Findings

1. **Database Schema**:
   - Column `operating_hours` (`jsonb`) exists in both `vendors` and `kitchens` tables (defaulting to `{"all": {"open": "08:00", "close": "20:00"}}`).
2. **Backend API**:
   - `merchantController.js` includes `operating_hours` column support, but `updateMerchantProfile` (`PATCH /api/v1/merchants/settings`) needed explicit handling when payload passes JSON object structure.
3. **Android App Gaps**:
   - `MerchantPortalScreen.kt` (`SettingsTabContent`) currently lacks UI components for viewing or editing Operating Hours. Merchants are unable to set or customize their store opening/closing times.
   - `apiService.updateMerchantSettings` does not include `operating_hours` in its request body.

---

## 🛠️ Proposed Changes

### Component 1: Android Mobile App (`:app`)

#### [MODIFY] [MerchantPortalScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/merchant/MerchantPortalScreen.kt)
- Add **Operating / Opening Hours** section in `SettingsTabContent`.
- Provide interactive Opening Time and Closing Time selectors (e.g. `08:00` to `20:00`).
- Pass `operating_hours` payload (`mapOf("all" to mapOf("open" to openTime, "close" to closeTime))`) to `apiService.updateMerchantSettings`.

#### [MODIFY] [StorefrontScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/commerce/StorefrontScreen.kt) & [ShopStorefrontScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/commerce/ShopStorefrontScreen.kt)
- Display Operating Hours badge (e.g. "🕒 Open: 08:00 AM - 08:00 PM") on store details headers.

---

### Component 2: Backend API Controller

#### [MODIFY] [merchantController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/merchantController.js)
- Update `updateMerchantProfile`:
  - Accept `operating_hours` in request body.
  - Store JSON stringified `operating_hours` in `vendors` or `kitchens` table.
- Return `operating_hours` in `getMerchantDashboard`.

---

## 🧪 Verification Plan

### Manual & Device Testing
1. Compile and deploy debug build to connected device (`192.168.1.2:42447`).
2. Login as a Merchant (Vendor or Kitchen) and open **Merchant Portal -> Settings**.
3. Change Opening Time to `07:30` and Closing Time to `21:30`, then save.
4. Verify Toast "Settings updated" appears and values persist on refresh.
5. Open Customer app -> Storefront and verify the updated Opening Hours badge displays correctly.
6. Commit changes to Git and push to GitHub `main`.
