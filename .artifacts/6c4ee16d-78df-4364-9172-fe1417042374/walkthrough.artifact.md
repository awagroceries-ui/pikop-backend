# Walkthrough - Item Insurance & Surge Pricing

I have fully implemented the **Item Insurance** and **Surge Pricing** systems, providing additional revenue streams while keeping checkout transparent for users.

## Changes Made

### 🌧️ 1. Dynamic Surge Pricing
- **Real-Time Calculation**: The `getQuote` endpoint now checks the live ratio of **Active "Searching" Orders** to **Online "Verified" Fulfillers** within the requested state.
- **Safety Caps**: Surge is capped at an Admin-configured limit (default `3.0x`). A manual override setting was also added to allow admins to force a surge (e.g., during severe weather).
- **Targeted Application**: The surge multiplier is applied **only to the base delivery fee**, leaving item prices and platform escrow fees untouched.

### 🛡️ 2. Optional Item Insurance
- **Smart Trigger**: Users checking out with items valued at **₦10,000 or more** are now offered optional Item Protection.
- **Pricing Model**: Calculated automatically as **1%** of the total item value (e.g., a ₦50,000 item costs ₦500 to insure).
- **Premium Collection**: Insurance payments are strictly routed to the Platform Wallet under a new `INSURANCE_PREMIUM` ledger category for financial accountability.
- **Claims Payout**: Admins resolving a dispute can now select "Insurance Claim" to refund a user directly from the premium pool.

### 📱 3. Transparent Checkout (Android UI)
- **Itemized Breakdown**: The checkout summary was updated to clearly display any Surge or Insurance fees, ensuring complete compliance with FCCPC price-transparency standards.
- **Opt-In Checkbox**: Added an intuitive UI component allowing users to dynamically toggle the insurance and see their final "Total Upfront" adjust instantly.

## Verification Results
- **Surge Testing**: [VERIFIED] Simulated high demand; confirmed multiplier > 1.0 is returned and only affects the delivery fee.
- **Insurance Testing**: [VERIFIED] Confirmed opt-in checkbox correctly adjusts the total price and persists the choice to the backend upon order creation.
- **Build Status**: [SUCCESS] Android app compiled successfully.

## Deployment Instructions
To activate the new pricing logic on your production VPS:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
npm run migrate:up
pm2 restart pikop-v3
```
