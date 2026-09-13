# Walkthrough - Merchant Growth: In-App Bulk Dispatch

I have successfully implemented the **In-App Bulk Dispatch** system, allowing high-volume merchants to create and manage large batches of delivery missions directly from their mobile app.

## New Capabilities

### 1. Manual Batch Creation Interface
- **The Screen:** Created `BulkDispatchScreen.kt`, a powerful tool for building delivery lists.
- **Dynamic Rows:** Merchants can now add multiple recipients, addresses, and item descriptions to a single batch.
- **Address Integration:** Each row is linked to the standard Map Address Search, ensuring delivery coordinates are accurate.

### 2. Unified Batch Payment & Activation
- **Single Click Dispatch:** Merchants can calculate the total cost for up to 50 missions and pay for them all at once using their wallet balance.
- **Automated Debiting:** The system automatically handles the financial ledger, debiting the merchant and activating all missions in the background.

### 3. Real-Time Monitoring in Merchant Portal
- **"Create Batch" Access:** Added a dedicated action button in the "Bulk" tab of the Seller Center.
- **Live Progress Tracking:** The portal now displays progress bars for active batches, showing the ratio of `processed` vs `total` orders in real-time.

### 4. Secure Session Bulk API
- **Direct App Access:** Implemented a new backend endpoint `POST /api/v1/merchants/orders/bulk-session` that works with the existing user login session.
- **Smart Validation:** The API ensures the user owns the merchant account and has sufficient funds before activating the batch.

## Verification Results

### Backend Implementation
- Verified syntax and route registration for the Bulk Session API.
- Confirmed wallet debiting logic in `merchantController.js`.
- **Result:** `PASS`.

### Android Build
- Successfully compiled the new Bulk Dispatch UI and integrated it with the Merchant Portal navigation.
- **Result:** `BUILD SUCCESSFUL`.

## Deployment Instructions (VPS)
Please apply these bulk dispatch and merchant growth updates to your **VPS**:

```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
pm2 restart pikop-v3
```
