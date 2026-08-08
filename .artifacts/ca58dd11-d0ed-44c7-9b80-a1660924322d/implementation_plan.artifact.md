# Implementation Plan - Admin Dashboard Branding & Creation

Professionalize the Admin Dashboard by applying the "Rich Black & Orange" brand theme and providing a secure method to create the initial administrative user.

## User Review Required

> [!IMPORTANT]
> - **Branding Consistency**: The Admin Dashboard will now match the Android app's premium theme (Black background, Orange accents).
> - **Admin Creation**: I will provide a one-time script to create your admin account. You should run this on your VPS once.

## Proposed Changes

### 1. Static Assets & Infrastructure (Backend)

#### [MODIFY] [app.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend/src/app.js)
- Add middleware to serve static files from a new `public` directory.
- This will allow the admin dashboard to load the brand logo.

#### [NEW] `backend/public/assets`
- I will create this directory and copy your brand logo (`pikop_logo.png`) into it.

---

### 2. UI Branding (Admin Views)

#### [MODIFY] [login.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend/src/views/login.ejs)
- Change background to `#0B0B0B`.
- Style the login card with a dark surface and orange highlights.
- Center the brand logo above the login form.

#### [MODIFY] [layout.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend/src/views/layout.ejs)
- Update the sidebar and main area to match the brand theme.
- Use Orange for active links and brand accents.

---

### 3. Admin Account Creation

#### [NEW] `backend/scratch/create_admin.js`
- A secure Node.js script that uses `bcrypt` to hash a password and insert a new admin user into the `admin_users` table.

## Verification Plan

### Manual Verification
- **Branding**: Open `https://api.awa.name.ng/admin/login` and verify it shows the new logo and theme.
- **Account Creation**: Run the provided script on the VPS and attempt to log in with the new credentials.
