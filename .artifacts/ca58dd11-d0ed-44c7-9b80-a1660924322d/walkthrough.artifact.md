# Walkthrough - Admin Dashboard Branding & Account Creation

I have successfully branded your Admin Dashboard to match the Pikop premium look and created a secure script for you to set up your first administrative account.

## Changes Made

### UI Branding
- **Branded Login**: Updated the `login.ejs` screen with your logo, a "Rich Black" background (`#0B0B0B`), and "Bright Orange" highlights (`#FF9F0A`).
- **Professional Layout**: Refined the `layout.ejs` (Dashboard wrapper) to include a branded sidebar, orange accent borders, and a clean dark theme.
- **Static Asset Serving**: Configured the backend to serve the brand logo and other assets to the web browser via a new `/public` route.

### Admin Account Management
- **Creation Script**: Created a new utility script `backend/scratch/create_admin.js` that allows you to securely create admin accounts from the VPS command line. It handles password hashing automatically using `bcrypt`.

## Deployment & Setup (On your VPS)

### 1. Update the code
Run these in your VPS terminal:
```bash
cd /var/www/pikop-api
git pull origin main
pm2 restart pikop-api
```

### 2. Create your Admin Account
Run the following command, replacing `<username>` and `<password>` with your choice:
```bash
# Example: node backend/scratch/create_admin.js moses mysecurepassword
node backend/scratch/create_admin.js <your_username> <your_password>
```

### 3. Log in
Go to `https://api.awa.name.ng/admin/login` and use the credentials you just created!

## Verification Results

### Automated Logic
- Verified that the `create_admin.js` script correctly hashes passwords before storage.
- Confirmed that the `app.js` correctly serves assets from the `backend/public` folder.

### Visual Check
- The Login screen and Sidebar now perfectly match your brand colors provided earlier.

> [!IMPORTANT]
> Keep your admin password secure. You can create multiple admin accounts with different roles (ops, super_admin, etc.) using the same script.
