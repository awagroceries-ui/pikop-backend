# Walkthrough - 100% Universal Tester Coupon (`TESTER100`)

I have successfully implemented the **100% Universal Tester Coupon (`TESTER100`)** across all four platform modules (Dispatch, Food, Groceries, Shop). Your testers and QA team can now place real orders without spending any actual money on payment gateways or wallet funds.

## Changes Made

### 🎟️ 1. Database Seeding
- **Migration**: Created `1726870000000_seed_universal_tester_coupon.js` to seed the active, unlimited coupon code **`TESTER100`** (`100% PERCENTAGE` discount).

### ⚙️ 2. Backend Pricing & Activation Engines
- **Order Controller (`orderController.js`)**: Updated `createOrder` so 100% percentage coupons waive the *entire* order fare (item price + delivery fee + platform fee). When `finalFare === 0`, the mission status is set to `'SEARCHING'` immediately without requiring Paystack or wallet balance.
- **Commerce Controller (`commerceController.js`)**: Updated `initializeCommerceOrder` to support zero-cost checkout. If `totalNaira === 0`, it bypasses Paystack initialization and immediately creates the order as `'PAID'` / `'SEARCHING'` in `orders` and populates `order_items`.
- **Payment Controller (`paymentController.js`)**: Hardened `initializePayment` with a 0-amount guard to activate free missions directly.

### 📱 3. Mobile App Integration
- **Order Quote Screen (`OrderQuoteScreen.kt`)**: Updated the promo calculation logic. Applying `TESTER100` reduces `amountToCharge` to **₦0.00**, triggering the zero-upfront bypass route.

---

## Verification Results

- **Syntax Checks**: [VERIFIED] All backend controllers passed Node.js syntax checks (`node -c`).
- **Release App Bundle**: [SUCCESS] Generated fresh production App Bundle at:
  `app/build/outputs/bundle/release/app-release.aab`
- **Git Push**: [SUCCESS] Commits pushed to `origin/main` (commit `35b07fa2`).

---

## Deployment Instructions

To activate the `TESTER100` coupon on your production VPS:

```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
npm run migrate:up
pm2 restart pikop-v3
```

Testers can now enter **`TESTER100`** in the promo field during checkout on any screen to place free orders! 🎟️
