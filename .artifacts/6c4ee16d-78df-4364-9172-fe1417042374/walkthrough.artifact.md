# Walkthrough - Marketplace Returns & Reverse Logistics

I have implemented a structured, end-to-end Marketplace Returns system that handles everything from merchant policy configuration to the automated refunding of funds after a successful return delivery.

## Changes Made

### 📋 1. Merchant Policy Control
- **Customizable Windows**: Merchants can now set their own `return_window_days` and toggle `allows_returns` in their settings.
- **Transparent Terms**: These policies are saved at the business level, allowing for future display on storefronts and at checkout.

### 🔄 2. The Return Request Flow
- **Customer Initiation**: Users can now request a return directly from their order history for any eligible marketplace mission within the valid window.
- **Evidence-Based Arbitration**: Requests include a mandatory reason and support for evidence URLs, which are then surfaced to the merchant for review.
- **Merchant Approval**: Merchants have a new **Returns** tab to review, approve, or decline incoming requests.

### 🚀 3. Reverse Dispatch & Flexible Billing
- **Automated Reverse Missions**: Upon approval, the system automatically generates a new **Reverse Dispatch mission**. It reverse-maps the pickup and delivery addresses to bring the item back to the merchant's doorstep.
- **Who Pays?**: I implemented the flexible billing request. Merchants can choose to **Cover the delivery fee** (e.g., for defective items) or have the **Customer pay** to activate the return trip.

### 💰 4. Receipt-Triggered Refunds
- **Automated Wallet Refund**: Once the merchant confirms receipt of the returned item via the portal, the system automatically triggers a refund of the original **Item Price** from the Merchant's wallet back to the Customer's wallet.
- **Safe Logistics**: The delivery fees from the original and return trips are preserved for the agents who performed the work, ensuring everyone is compensated fairly.

## Verification Results
- **Billing Logic**: [VERIFIED] Merchant-paid returns correctly debit the merchant wallet and start the mission in `SEARCHING` status. Customer-paid returns wait in `AWAITING_PAYMENT`.
- **Eligibility Engine**: [VERIFIED] Orders outside the merchant's window are correctly blocked from return requests.
- **Build Status**: [SUCCESS] Successfully compiled and verified the Android app.

## Deployment Instructions
To activate the reverse logistics engine on your production VPS:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
npm run migrate:up
pm2 restart pikop-v3
```
