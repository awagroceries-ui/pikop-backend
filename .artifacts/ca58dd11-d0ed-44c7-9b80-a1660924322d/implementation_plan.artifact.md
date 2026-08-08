# Implementation Plan - Admin Dashboard Visual Redesign

Redesign the Pikop Admin Dashboard into a "Mission Control" interface optimized for long shifts and rapid decision-making, including real-time trends and performance charts.

## Design Strategy: "Pikop Terminal 1.0"

### Visual Direction
- **Color Palette**:
    - **Background**: `#0F172A` (Midnight Navy) - High contrast but lower eye strain than pure black.
    - **Moving (Active)**: `#38BDF8` (Sky Blue) - Represents the fleet in motion.
    - **Waiting (Pending)**: `#FBBF24` (Amber) - Needs attention soon.
    - **Stalled (Critical)**: `#EF4444` (Signal Red) - Blocked or error state.
    - **UI Accents**: `#1E293B` (Slate) - Subtle depth and compartmentalization.
- **Typography**:
    - **Display**: Inter (Sans-serif) for high scannability.
    - **Data/Numbers**: JetBrains Mono (Monospaced) for perfectly aligned tabular figures (fares, counts).
- **Layout**: "Information Waterfall" grid. Pulse KPIs at the top, Live Order Board (Mission Map equivalent) in the center, and Historical Trends at the bottom.
- **Signature Element**: **"The Velocity Pulse"**. Order status badges will include a subtle animation pulse if they exceed the "Normal" threshold for their current state (e.g., searching for > 5 mins).

### Critique vs Generic Clusters
- **vs Warm/Clay**: Rejected. Too "lifestyle/consumer." Logistics needs tactical precision.
- **vs Black/Neon**: Rejected. Midnight Navy is softer on the eyes for 8-hour shifts than pure black `#000000`.
- **vs Broadsheet Hairlines**: Rejected. Moderate rounding (`0.75rem`) and soft Slate borders provide a "hardware/tool" feel rather than a newspaper.

---

## Proposed Changes

### Backend: Analytics & Reports

#### [MODIFY] [adminRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend/src/routes/adminRoutes.js)
- Add `GET /admin/reports/revenue` (JSON endpoint for Chart.js).
- Add `GET /admin/reports/orders` (JSON endpoint for Chart.js).

#### [MODIFY] [adminController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend/src/controllers/adminController.js)
- Implement `getRevenueReport`: Aggregates platform earnings by day for the selected range.
- Implement `getOrderReport`: Aggregates volume (Success vs Cancelled) by day.
- Extend `getDashboard`: Provide historical data for sparklines in KPI tiles.

---

### Presentation Layer: Redesign

#### [MODIFY] [layout.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend/src/views/layout.ejs)
- Inject Google Fonts (Inter & JetBrains Mono).
- Update Global CSS:
    - Custom scrollbars.
    - Dark theme overrides for Bootstrap.
    - "Velocity Pulse" animations.
- Include **Chart.js** via CDN.

#### [MODIFY] [dashboard.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend/src/views/dashboard.ejs)
- Redesign KPI Tiles to include inline 7-day sparklines.
- Add "Platform Performance" section with Revenue and Volume line charts.
- Implement designed "Empty States" for zero-activity periods.

#### [MODIFY] [orders.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend/src/views/orders.ejs)
- Update table to use monospaced figures.
- Implement "The Velocity Pulse" visual indicators on stalled orders.

#### [MODIFY] [kyc_queue.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend/src/views/kyc_queue.ejs)
- Update empty state to "All caught up" messaging with a brand-aligned illustration/icon.

---

## Verification Plan

### Manual Verification
- **Visual Audit**: Verify contrast ratios against WCAG 2.1 (AA) for readability.
- **Responsiveness**: Verify dashboard layout on iPad Pro (Tablet) resolution.
- **Data Integrity**: Compare Chart.js totals against raw database queries for revenue.
- **A11y**: Test keyboard navigation focus states (especially on action buttons like KYC approval).
