# Implementation Plan - Restrict Promo Codes to Delivery Fee & UI Breakdown

This plan ensures that promo code discounts are only applied to the delivery fee and updates the Order Summary UI to show a clear breakdown of costs.

## Problem Description
1.  **Promo Scope:** Promo codes are currently applied to the full order total, which could incorrectly discount the COD item price or platform fees.
2.  **UI Clarity:** The current order summary does not clearly distinguish between the item/fee portion and the delivery portion, making the promo code's impact less obvious.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- **`createOrder`**:
    - Update logic to calculate discount based strictly on `delivery_fee`.
    - `discount = min(promoDiscount, delivery_fee)`.
    - `finalFare = item_price + (delivery_fee - discount) + platform_fee_amount` (if payer pays fee).
    - Ensure `original_delivery_fee` stores the pre-discount value.

#### [MODIFY] [paymentController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/paymentController.js)
- **`activatePaidMission`**:
    - Align activation logic with the same restricted promo calculation.
    - Since `quotes` table only has `total_fare`, we will prioritize using the `item_price` and `delivery_fee` passed in the Paystack `metadata`.

---

### Android App

#### [MODIFY] [OrderQuoteScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/OrderQuoteScreen.kt)
- **Calculation Logic:**
    - Update `discount` calculation to be `min(calculatedDiscount, result.delivery_fee)`.
    - Update `totalPayable` to be `fullItemPrice + (deliveryFee - discount) + platformFee`.
- **UI Update:**
    - Refactor the Order Summary card to show two distinct sections:
        1. **COD Item & Fees:** Item price, Platform fee (if applicable), and Subtotal.
        2. **Delivery:** Delivery fee, Promo discount (if any), and Delivery total.
    - Show the final "Total Payable" at the bottom.
    - Use the same breakdown logic for the UI both before and after applying a promo.

---

## Verification Plan

### Automated Tests
- Run Android build: `./gradlew assembleDebug`.
- Syntax check backend: `node -c src/controllers/orderController.js`.

### Manual Verification
1.  **Small Promo:** Apply a ₦200 promo on a ₦1000 delivery fee. Total should decrease by exactly ₦200.
2.  **Large Promo:** Apply a ₦2000 promo on a ₦1000 delivery fee. Delivery fee should floor at ₦0, and the item price/platform fee should remain untouched.
3.  **UI Breakdown:** Verify that the "COD Item & Fees" and "Delivery" sections are visually separated and sum up correctly to the "Total Payable".
