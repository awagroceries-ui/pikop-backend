# Walkthrough - Pikop Commerce Phase 4: Unified Checkout

I have successfully implemented the final phase of the Pikop Commerce module. Users can now purchase marketplace products or kitchen meals and have them automatically dispatched for delivery in a single, seamless transaction.

## New Capabilities

### 1. Seamless "Buy & Deliver" Experience
- **The Flow:** When a user clicks an item in the Storefront, they are taken to a new `CommerceCheckoutScreen.kt`.
- **Integrated Addresses:** Users can select a delivery address from their saved locations or use the map.
- **Auto-Pricing:** The system automatically calculates the delivery fare from the **Merchant's shop** to the **User's house** based on distance.

### 2. Unified Paystack Transaction
- **Single Payment:** Users pay for both the **item cost** and the **delivery fee** in one Paystack session.
- **Financial Audit:** The backend correctly splits the payment:
    - **Item Price:** Held in escrow for the Merchant.
    - **Delivery Fee:** Held for the Agent (75%) and Platform (25%).

### 3. Automated Dispatch Engine
- **Instant Activation:** The moment a commerce payment is confirmed via webhook, the system **automatically creates a delivery mission**.
- **Agent Discovery:** The mission is instantly broadcasted to nearby agents, who see it as a high-priority "Marketplace Delivery" offer.

### 4. Merchant Protection & Escrow
- **Safe Sales:** Funds for the item are secured by the platform.
- **Release Terms:** Payment is only credited to the Merchant's withdrawable balance once the customer confirms they have received the item.

## Verification Results

### Backend Integrity
- Verified the `COMMERCE_ORDER` webhook handler in `paymentController.js`.
- Verified `initializeCommerceOrder` distance logic in `commerceController.js`.
- **Result:** `PASS`.

### Android Build
- Successfully integrated the Storefront, Checkout, and Address selection.
- **Result:** `BUILD SUCCESSFUL`.

## Deployment Instructions (VPS)
Please apply this final commerce integration to your **VPS**:

```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
npm run migrate:up
pm2 restart pikop-v3
```
