# Walkthrough - Merchant Analytics Dashboard

I have implemented a professional Analytics Dashboard for Merchants, giving them a data-driven reason to stay engaged with the Pikop platform.

## Changes Made

### 🧠 1. Server-Side Aggregation (Backend)
- **New Analytics Endpoint**: Added `getMerchantAnalytics` to `merchantController.js`. It performs high-performance SQL aggregations for:
    - **Sales Trend**: Net revenue and order volume over time.
    - **Best Sellers**: Ranking of items by units sold and contribution to net revenue.
    - **Customer Retention**: Calculating the percentage of repeat buyers.
    - **Peak Times**: Identifying the busiest days and hours for the merchant's business.
- **Time Range Flexibility**: The backend now supports dynamic grouping by `daily`, `weekly`, `monthly`, and `annual` periods, respecting the Africa/Lagos (WAT) timezone.
- **Financial Accuracy**: All revenue metrics are calculated **net of Marketplace Commission**, ensuring the analytics match the merchant's actual wallet earnings.

### 📱 2. Insights Dashboard (Android UI)
- **New Insights Tab**: Integrated a dedicated "Insights" tab into the Merchant Portal.
- **Range Selector**: Added a quick toggle for merchants to switch between time windows.
- **Visual KPI Cards**: Summarized Net Revenue, Order Count, and Repeat Customer rates for immediate visibility.
- **Popularity & Peak Lists**: Built structured lists for "Best Selling Items" and "Peak Hours," allowing merchants to optimize their inventory and operating hours.
- **UI Optimization**: To keep the navigation clean, I moved the **Business Settings** to a dedicated gear icon in the Top Bar, freeing up space in the main tab row.

## Verification Results
- **Merchant Isolation**: [VERIFIED] SQL queries strictly filter by the authenticated `seller_id`. Merchant A cannot see data from Merchant B.
- **Time Range Sync**: [VERIFIED] Toggling between Week and Month correctly updates the trend periods and total aggregates.
- **Net Revenue Check**: [VERIFIED] Verified that commission deductions are accurately reflected in the reported revenue figures.
- **Build Status**: [SUCCESS] Successfully compiled and verified the Android app.

## Deployment Instructions
To activate the analytics engine on your production VPS:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
pm2 restart pikop-v3
```
