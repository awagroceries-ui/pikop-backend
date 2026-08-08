# Admin Management & UI Fix

## Backend: Middleware & Logic
- [x] Implement `res.locals` middleware in `adminRoutes.js`
- [x] Implement `getAdmins`, `addAdmin`, and `deleteAdmin` in `adminController.js`
- [x] Clean up existing controller `render` calls

## Frontend: Admin UI
- [x] Create `admins.ejs` view
- [x] Update `layout.ejs` sidebar with "Manage Staff"
- [x] Finalize "Mission Control" UI consistency across all tabs

## Verification
- [ ] Test tab rendering (Fix "code only" issue)
- [ ] Test admin creation and deletion
- [ ] Test role-based access control (RBAC)
