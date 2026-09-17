# Implementation Plan - Merchant Analytics Dashboard

This plan implements a dedicated Analytics view for Merchants, providing insights into sales performance, customer retention, and peak operational windows.

## Proposed Changes

### 1. Backend Logic (Node.js)

#### [MODIFY] [merchantController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/merchantController.js)
- Implement `getMerchantAnalytics`:
    - **Time Range Handling**: Supports `daily`, `weekly`, `monthly`, and `annual` aggregations in WAT.
    - **Sales & Volume Trend**: Aggregates total missions and **Net Revenue** (Item Price - Merchant Commission) grouped by time period.
    - **Best Sellers**: Top 5 items ranked by units sold and revenue.
    - **Retention**: Calculates the percentage of unique customers who have placed more than one order.
    - **Operational Peaks**: Identifies the top 5 hour/day slots with the highest order volume.

#### [MODIFY] [merchantRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/merchantRoutes.js)
- Register `GET /analytics` endpoint.

---

### 2. Android App Integration (Compose)

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Add `MerchantAnalyticsResponse` and nested DTOs (`TrendItem`, `BestSeller`, `PeakTime`, `RetentionStats`).
- Add `getMerchantAnalytics` endpoint.

#### [MODIFY] [MerchantPortalScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/merchant/MerchantPortalScreen.kt)
- Add a new **Analytics** tab (position 2, moving others down).
- **Redesign Tab Row**: Ensure it handles 6 tabs gracefully or move settings to a profile button. I will move "Settings" to a top-bar action to keep the tab row clean (Sales, Listings, Analytics, Returns, Bulk).

#### [NEW] [MerchantAnalyticsScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/merchant/MerchantAnalyticsScreen.kt)
- **Time Range Picker**: Segmented buttons for Day/Week/Month/Year.
- **KPI Dashboard**: Summary cards for Total Net Revenue and Order Count.
- **Performance Visualization**: A scrollable trend list showing growth over the selected period.
- **Operational Insights**: "Most Popular Items" list and "Busiest Hours" breakdown.

---

## User Review Required

> [!IMPORTANT]
> **Financial Consistency**
> All revenue figures shown to the Merchant will be **Net Revenue** (what they actually receive after Pikop's commission is deducted). This ensures the dashboard matches their wallet balance.

> [!NOTE]
> **Data Privacy**
> Analytics are strictly scoped to the authenticated merchant. Cross-merchant data visibility is blocked at the SQL query level using `WHERE seller_id = $userId`.

## Verification Plan

### Manual Verification
1.  **Merchant Isolation**: Log in as Merchant A. Verify analytics only show orders where Merchant A was the seller.
2.  **Trend Accuracy**: Compare the "Total Net Revenue" in Analytics against the sum of "Net Payouts" in the Sales tab for the same period.
3.  **Range Switching**: Toggle between "Weekly" and "Monthly". Verify that data points update correctly (e.g., daily points for weekly, monthly points for annual).
4.  **Best Sellers**: Sell 3 units of Item X and 1 unit of Item Y. Verify Item X appears at the top of the "Best Sellers" list.
