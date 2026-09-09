# Walkthrough - Promo Code Scope & Order Summary UI Refactor

I have restricted the application of promo codes to the delivery fee only and refactored the Order Summary UI to show a clear breakdown of costs.

## Changes Made

### 1. Promo Code Scope Restriction
- **Logic Change:** Updated both the Android app and the backend (`orderController.js`, `paymentController.js`) to ensure promo codes only discount the `delivery_fee`.
- **Capping:** If a promo discount is larger than the delivery fee, it is now capped at the delivery fee amount (flooring at ₦0). It will no longer spill over into discounting the `item_price` or `platform_fee_amount`.
- **Integrity:** `item_price` and `platform_fee_amount` are now guaranteed to be charged in full, protecting the escrow and platform revenue.

### 2. Order Summary UI Refactor
- **Cost Breakdown:** The summary card in `OrderQuoteScreen.kt` now displays costs in two distinct sections:
    1.  **COD Item & Fees:** Clearly lists the Item price, Platform fee (if applicable), and a section subtotal.
    2.  **Delivery:** Shows the Delivery fee, the Promo discount (if applied), and the final Delivery subtotal.
- **Visual Feedback:** This breakdown provides immediate feedback when a promo code is applied, making it obvious that only the delivery portion is being discounted.
- **Consistency:** The same breakdown logic is used for the final payment confirmation, ensuring no discrepancies between the quote and checkout.

## Verification Results

### Automated Build
- Ran `./gradlew assembleDebug`.
- **Result:** `BUILD SUCCESSFUL`.

### Deployment Instructions (For User)
Please apply these logic fixes to your **VPS**:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
pm2 restart pikop-v3
```

### Manual Verification Steps
1.  **Small Promo Test:** Apply a ₦100 promo to a ₦1000 delivery. Verify the total payable decreases by exactly ₦100.
2.  **Large Promo Test:** Apply a 100% discount promo. Verify the "Delivery" section shows a subtotal of ₦0, but the "COD Item & Fees" section remains unchanged.
3.  **UI Verification:** Confirm that the Order Summary now shows the headers "COD Item & Fees" and "Delivery" with their respective line items.
