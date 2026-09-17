# Implementation Plan - Item Insurance & Surge Pricing

This plan implements two new financial features: **Optional Item Insurance** for high-value deliveries and **Dynamic Surge Pricing** for high-demand periods.

## Proposed Changes

### 1. Database & Infrastructure

#### [NEW] [pricing_enhancements migration](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1726550000000_pricing_enhancements.js)
- **Extend `quotes` and `orders`**:
    - Add `insurance_fee` (DECIMAL).
    - Add `is_insured` (BOOLEAN).
    - Add `surge_multiplier` (DECIMAL, default 1.0).
- **Update `wallet_ledger_entries`**: Add `INSURANCE_PREMIUM` and `INSURANCE_CLAIM` to the purpose check constraint.
- **Seed Settings**:
    - `insurance_rate`: 0.01 (1%)
    - `insurance_min_item_value`: 10000
    - `max_surge_multiplier`: 3.0

### 2. Backend Logic (Node.js)

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- **`getQuote` Enhancements**:
    - **Surge Calculation**: Implement real-time demand/supply check per state.
        - Formula: `surge = (ActiveOrders + 1) / (OnlineFulfillers + 1)`.
        - Apply multiplier to the base delivery fee.
    - **Insurance Calculation**: If `item_price >= insurance_min_item_value`, calculate `insurance_fee = item_price * insurance_rate`.
- **`createOrder` Enhancements**:
    - Capture `is_insured` opt-in from request.
    - Persist final calculated fees and multiplier.

#### [MODIFY] [walletService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/walletService.js)
- Update settlement logic to credit `INSURANCE_PREMIUM` to the platform wallet.
- Implement `processInsuranceClaim(orderId)` for admin resolution of lost/damaged insured items.

### 3. Admin Dashboard

#### [MODIFY] [adminController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/adminController.js)
- Update settings page to include Insurance and Surge limits.
- Implement manual surge override setting (e.g., `manual_surge_multiplier` setting).

### 4. Android Frontend (Compose)

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Update `QuoteResponse` and `CreateOrderRequest` to include insurance and surge fields.

#### [MODIFY] [OrderQuoteScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/OrderQuoteScreen.kt)
- **Insurance UI**: Show an "Item Protection" checkbox if the item value is above the threshold. Display the exact cost.
- **Surge UI**: If `surge_multiplier > 1.0`, show a "High Demand" notice next to the delivery fee (e.g., "₦1,500 (Includes 1.2x surge)").

## User Review Required

> [!IMPORTANT]
> **Pricing Decisions**
> 1. **Insurance**: 1% of item price (₦100 for a ₦10k item). Minimum value threshold of ₦10,000.
> 2. **Surge**: Applied to **Delivery Fee only**. Transparently disclosed at checkout. Capped at 3.0x.

## Verification Plan

### Manual Verification
1.  **Surge Test**: Simulate high demand (create 10 searching orders, keep 1 fulfiller online). Verify `getQuote` returns a multiplier > 1.0 and delivery fee increases.
2.  **Insurance Test**: Set item price to ₦20,000. Verify the "Protect this item" option appears with a ₦200 fee. Complete order and verify `is_insured: true` in DB.
3.  **Claim Test**: Confirm an insured order. As Admin, trigger an insurance claim settlement. Verify platform wallet is debited and user is credited under `INSURANCE_CLAIM`.
