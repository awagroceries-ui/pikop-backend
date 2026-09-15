# Implementation Plan: Merchant COD Opt-In/Opt-Out

This plan adds a setting for merchants to decide whether they want to accept Cash on Delivery (COD) orders. This preference is collected during onboarding and enforced during customer checkout.

## User Review Required

> [!IMPORTANT]
> **Default Setting for Existing Merchants**
> I will default `accepts_cod` to `true` for all existing and new merchants to maintain current behavior while rolling out the feature.

> [!NOTE]
> **Cart Architecture**
> The current `CommerceCheckoutScreen.kt` appears to handle single-item checkouts. I will enforce the `accepts_cod` check for the specific item being purchased. If Pikop transitions to multi-item carts, a "prepaid-only if any item is prepaid-only" logic will be easier to implement then.

## Proposed Changes

### Database Layer
- **[NEW] Migration**: `1726410000000_add_merchant_cod_toggle.js`
  - Add `accepts_cod` BOOLEAN column (default `true`) to `vendors` and `kitchens` tables.

### Backend Layer
#### [MODIFY] `merchantController.js`
- `setupMerchantProfile`: Accept `accepts_cod` in request body and save to DB.
- `getMerchantProfile`: Include `accepts_cod` in the returned profile object.
- **[NEW]** `updateMerchantSettings`: A new endpoint to allow merchants to toggle `accepts_cod`.

#### [MODIFY] `merchantRoutes.js`
- Register `PATCH /api/v1/merchants/settings` for `updateMerchantSettings`.

#### [MODIFY] `commerceController.js`
- `getDiscovery`: Update SQL to JOIN with `vendors`/`kitchens` and return the `accepts_cod` flag for each item.

### Android API Layer
#### [MODIFY] `ApiService.kt`
- Update `SetupMerchantRequest` to include `accepts_cod`.
- Update `MerchantProfile` to include `accepts_cod`.
- Update `DiscoveryItem` to include `accepts_cod`.
- Add `updateMerchantSettings` method.

### Android UI Layer
#### [MODIFY] `MerchantBusinessSetupScreen.kt` (Onboarding Stage 2)
- Add a "Accept COD Orders" toggle with an explanation: "Funds are held in escrow and released after delivery. A 10% platform fee is paid by the buyer."

#### [MODIFY] `CommerceCheckoutScreen.kt`
- Check `item.accepts_cod`. If `false`, hide the "Pay on Delivery" card and default `selectedPaymentMethod` to `CARD`.

#### [MODIFY] `MerchantPortalScreen.kt`
- Add a new "Settings" tab (or a section in existing tabs) to allow toggling `accepts_cod`.

## Verification Plan

### Automated/Code Verification
- Verify successful Gradle build of the Android app.
- Verify DB migration runs without errors.

### Manual Verification
1. **Onboarding**: Sign up as a new merchant, toggle COD to "Off", and complete verification.
2. **Settings**: Go to Merchant Portal -> Settings, toggle COD back to "On", then back to "Off".
3. **Checkout (Merchant OFF)**: As a customer, attempt to buy an item from a merchant who has COD disabled. Verify only "Pay Now" is visible.
4. **Checkout (Merchant ON)**: As a customer, buy from a merchant with COD enabled. Verify both "Pay Now" and "Pay on Delivery" are visible.
5. **Data Integrity**: Verify that changing the setting doesn't affect existing orders.