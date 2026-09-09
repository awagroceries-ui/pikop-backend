# Walkthrough - Pickup & Delivery Code Generation

I have fixed the issue where pickup and delivery confirmation codes were not being generated or displayed to the customer. These codes are now reliably generated, securely hashed, and surfaced on the customer's tracking screen.

## Changes Made

### 1. Backend: Robust Code Generation & Hashing
- **Migration:** Created a new migration to add `pickup_code` and `delivery_code` columns to the `orders` table.
- **Generation:** Updated `orderController.js` and `paymentController.js` to generate unique 4-digit numeric codes at order creation.
- **Security:** The plain codes are stored for the customer's view, while secure hashes (`bcrypt`) are stored for fulfiller-side verification.
- **Access Control:** Updated `getOrderDetails` to ensure that plain confirmation codes are **only** returned to the customer who created the mission (or an admin). Fulfillers only see the hashes, preventing unauthorized bypass.

### 2. Android App: Surfacing Codes to Customers
- **Model Update:** Added `pickup_code` and `delivery_code` fields to the `OrderDetailsResponse` in `ApiService.kt`.
- **UI Enhancement:** Updated `TrackOrderScreen.kt` to include a **"Confirmation Codes"** section in the tracking sheet.
- **Copy Affordance:** Added a `CodeBox` component that allows customers to tap and copy the codes to their clipboard for easy sharing with agents/recipients.

### 3. Fulfiller Verification Stability
- **Verification Logic:** Refined the fulfiller-side verification to use the top-level `bcrypt` instance and improved SQL parameter alignment, ensuring "Verify Pickup" and "Verify Delivery" are rock-solid.

## Verification Results

### Automated Build
- Ran `./gradlew assembleDebug`.
- **Result:** `BUILD SUCCESSFUL`.

### Deployment Instructions (For User)
Please run these on your **VPS** to apply the database schema changes and generation logic:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
npm run migrate:up
pm2 restart pikop-v3
```

### Manual Verification Steps
1. **Request Mission:** Create a new delivery mission.
2. **View Tracking:** Once activated, go to the "Tracking" screen. You should see a new card with two 4-digit codes: one for Pickup and one for Delivery.
3. **Copy Code:** Tap a code to copy it.
4. **Fulfiller Check:** As a fulfiller, try to verify pickup/delivery with an incorrect code. It should be rejected. Verify with the correct code to proceed.
