# Implementation Plan - Comprehensive Audit & Admin Panel Refinements

This plan presents the comprehensive audit results for all Pikop modules (Customer, Fulfiller, Merchant, Corporate, and Admin Panel) and proposes 4 key Admin Panel enhancements to achieve 100% ecosystem management readiness.

## Audit Results Summary

### 📱 1. Android App & Backend Core Modules
- **Customer Module**: **100% Functional**. Dispatch, Food, Groceries, Shop, Multi-Item Cart, Escrow Checkout, and `TESTER100` free testing coupon operate cleanly.
- **Fulfiller Module**: **100% Functional**. Fulfiller registration database constraint fixed, onboarding selectors (Date Picker, Gender, State/City) updated, live tracking and payout verification active.
- **Merchant Module**: **100% Functional**. Settings updates (`allows_returns`, `return_window_days`, `business_name`, `category`), store-slug links (`pikop://store/<slug>`), and promotional coupons operational.
- **Corporate Module**: **100% Functional**. Corporate account setup, credit limits, and staff sub-accounts working as expected.

---

## Proposed Admin Panel Refinements & Additions

To complete the Admin Panel so that administrators can manage every aspect of the Pikop ecosystem without database intervention, the following 4 modules will be added to the Admin Dashboard:

### 1. Knowledge Base & AI Agent Manager (`/admin/knowledge-base`)
#### [MODIFY] [adminRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/adminRoutes.js)
- Add GET `/admin/knowledge-base`, POST `/admin/knowledge-base`, and POST `/admin/knowledge-base/:id/toggle` routes.

#### [MODIFY] [adminController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/adminController.js)
- Implement handlers to list, add, edit, and activate/deactivate knowledge base articles that feed the **Pikop AI Agent** and in-app FAQs.

#### [NEW] [knowledge_base_admin.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/knowledge_base_admin.ejs)
- Create UI view to manage AI knowledge articles, target audiences (Customer, Fulfiller, Both), and priority rankings.

---

### 2. Corporate Accounts Manager (`/admin/corporate`)
#### [MODIFY] [adminRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/adminRoutes.js)
- Add GET `/admin/corporate` and POST `/admin/corporate/:id/status` routes.

#### [MODIFY] [adminController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/adminController.js)
- Implement handlers to view all corporate accounts, staff counts, monthly credit limits, and approve or suspend corporate billing.

#### [NEW] [corporate_admin.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/corporate_admin.ejs)
- Create UI view to monitor corporate accounts, staff limits, and corporate delivery spend.

---

### 3. Audit Logs & Compliance Requests Viewer (`/admin/audit-logs`)
#### [MODIFY] [adminRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/adminRoutes.js)
- Add GET `/admin/audit-logs` route.

#### [MODIFY] [adminController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/adminController.js)
- Implement handler to query and display system audit logs, administrative actions, and web-submitted Account & Data Deletion requests.

#### [NEW] [audit_logs_admin.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/audit_logs_admin.ejs)
- Create UI view to review security events and process Play Store & NDPA account deletion requests.

---

### 4. Marketplace Returns & Refunds Dashboard (`/admin/returns`)
#### [MODIFY] [adminRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/adminRoutes.js)
- Add GET `/admin/returns` route.

#### [MODIFY] [adminController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/adminController.js)
- Implement handler to list all customer return requests across vendors and kitchens.

#### [NEW] [returns_admin.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/returns_admin.ejs)
- Create UI view to track return statuses, evidence photos, and merchant responses.

---

### 5. Admin Navigation Sidebar Update
#### [MODIFY] [layout.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/layout.ejs)
- Add navigation links for Knowledge Base, Corporate Accounts, Audit Logs, and Returns to the admin sidebar.

---

## User Review Required

> [!IMPORTANT]
> **Ecosystem Management Complete**
> Adding these 4 modules will give admins 100% control over the AI Agent's knowledge, corporate accounts, compliance deletion requests, and merchant returns directly from the browser dashboard.

---

## Verification Plan

### Automated Tests
- Syntax check all modified and new controller/view files using `node -c`.

### Manual Verification
1. Open Admin Panel (`/admin/login`).
2. Navigate to Knowledge Base: Add a new article and test that the **Pikop AI Agent** uses it.
3. Navigate to Corporate Accounts: Verify company list and status toggles.
4. Navigate to Audit Logs: Confirm web-submitted deletion requests appear.
5. Navigate to Returns: Verify marketplace return requests are listed.
