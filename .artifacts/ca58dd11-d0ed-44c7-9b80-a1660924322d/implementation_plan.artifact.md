# Implementation Plan - Admin Management & UI Bug Fix

Fix the "code only" display issue on dashboard tabs and implement a new feature to manage administrative users.

## User Review Required

> [!IMPORTANT]
> - **Fixing the UI**: The "code only" issue was likely caused by missing variables (like `role`) in the EJS templates, causing rendering failures. I will implement a centralized way to handle these variables.
> - **Manage Admins**: This will allow Super Admins to create new accounts (Ops, Finance, etc.) directly from the dashboard instead of using terminal scripts.

## Proposed Changes

### 1. Fix Dashboard Rendering (Backend)

#### [MODIFY] [adminRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend/src/routes/adminRoutes.js)
- Add a middleware to automatically populate `res.locals.admin` and `res.locals.role` for all authenticated admin routes. This ensures EJS always has the required variables for the layout.

#### [MODIFY] [adminController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend/src/controllers/adminController.js)
- Implement `getAdmins`: Fetch all administrative users.
- Implement `addAdmin`: Create a new admin with a hashed password and selected role.
- Implement `deleteAdmin`: Remove an admin account.
- Clean up all `res.render` calls to remove redundant variable passing.

---

### 2. Admin Management Feature (UI)

#### [NEW] `backend/src/views/admins.ejs`
- A professional table showing all admin users.
- A "Add New Admin" form with role selection (super_admin, ops, finance, support).
- "Delete" action for removing staff access.

#### [MODIFY] [layout.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend/src/views/layout.ejs)
- Add "Manage Staff" link to the sidebar.
- Ensure the `role` variable is safely handled if it's undefined (using `locals.role`).

---

## Verification Plan

### Manual Verification
- **UI Fix**: Click through all tabs (Order Board, Financials, etc.) and verify they render the full "Mission Control" UI instead of raw code.
- **Admin Management**: Create a new 'Ops' user, log out, and verify you can log in with the new credentials.
- **Role Enforcement**: Verify that a 'Support' user cannot access the "Manage Staff" or "Settings" sections.
