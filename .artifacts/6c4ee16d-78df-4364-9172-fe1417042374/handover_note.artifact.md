# Comprehensive Handover Note: Project Pikop Logistics & Commerce V3

This handover note serves as the definitive reference for a new chat assistant to take over Project Pikop without losing context, history, architecture, or configuration details.

---

## 🚀 1. Project Overview & Architecture

- **Project Name**: Pikop Logistics (Android App & Backend API v3).
- **Core Architecture**:
  - **Android Client (`/app`)**: Jetpack Compose, Material 3, Hilt (DI), Retrofit, Google Maps SDK, Coroutines.
  - **Backend API (`backend_v3`)**: Node.js, Express, PostgreSQL, Socket.io, EJS Admin views, Axios, Paystack, Google Places API.
  - **Production Server**: Ubuntu VPS (`srv1932412`), running via PM2 (`pikop-v3`), URL: `https://api.pikop.com.ng/`.

---

## 📊 2. Recent Major Milestones & Progress

### ⚖️ Legal & Policy Hardening
- **Standardized Terms**: Implemented a comprehensive 12-section T&C and NDPA-aligned Privacy Policy (Bolt/Uber style).
- **Hold Harmless**: Added a robust indemnification clause to protect Awa Foods & Groceries.
- **Reporting & Disposal**: Explicit policy to report illegal goods to police and discard items immediately without refund.
- **Financial Policies**: Enforced 25% pre-pickup cancellation fee and 75% return fee.

### 💰 Fleet Performance & Automated Payouts
- **Instant Payouts**: Integrated Paystack Transfer API. Withdrawals ≤ ₦5,000 are processed automatically/instantly.
- **Insights Dashboard**: Fulfillers now see Completion Rate (%), MTD Earnings, and a 7-day visual earnings chart.
- **Tier Engine**: Nightly background job (`fleetJob.js`) audits performance to promote/demote agents (Gold, Silver, Bronze) and flag risks.

### 📦 Merchant Growth & Commerce
- **In-App Bulk Dispatch**: Senders can now build delivery lists row-by-row and pay for a whole batch at once via wallet balance.
- **Merchant Onboarding**: Comprehensive registration flow for "General Vendors" and "Cloud Kitchens."
- **Inventory Management**: Full CRUD interface for merchants to list products/meals with photo uploads.
- **Marketplace Discovery**: Unified search hub with proximity sorting (closest shops first) and category filtering.

---

## 🔑 3. Working Configurations, Keys & `.env` Details

### Android (`local.properties` in project root)
```properties
sdk.dir=C\:\\Users\\MOSES\\AppData\\Local\\Android\\Sdk
googleMapsApiKey=AIzaSyDwCHF7qF7IjfMLnqxTl1YWjDjxPogu9RM
paystackPublicKey=pk_live_...
```

### Backend VPS (`/var/www/pikop-api/backend_v3/backend_v3/.env`)
```env
PORT=3000
NODE_ENV=production
DATABASE_URL=postgres://...
PAYSTACK_SECRET_KEY=sk_live_...
GOOGLE_MAPS_API_KEY=AIzaSyDwCHF7qF7IjfMLnqxTl1YWjDjxPogu9RM
GOOGLE_PLACES_API_KEY=AIzaSyDwCHF7qF7IjfMLnqxTl1YWjDjxPogu9RM
```

---

## 🤖 4. Critical Automation Protocol (MANDATORY)

### 🛠️ Build Strategy
- **NEVER** use the IDE's built-in "Run" button. It is unreliable for this complex project.
- **ALWAYS** run builds using the `gradle_build` tool or direct ADB commands in a shell:
  ```bash
  ./gradlew assembleDebug --no-daemon
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  adb shell am start -n com.ng.pikop/com.ng.pikop.MainActivity
  ```

### 📦 Version Control
- **ALWAYS** carry out all Git tasks automatically after every logic or UI change. Do not wait for user prompts.
  ```bash
  git add .
  git commit -m "<Detailed Milestone description>"
  git push origin main
  ```

### 🚀 VPS Deployment
When backend changes are pushed, provide the user with the exact block to copy-paste into their VPS:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
npm run migrate:up # If new migrations exist
pm2 restart pikop-v3
```

---

## 📍 5. Current Priority: Marketplace Module Separation
The next task is to restructure the home screen into 4 distinct modules:
1. **Dispatch** (Parcel delivery)
2. **Kitchen** (Food/Meals)
3. **Groceries** (Raw/Pantry)
4. **Marketplace** (General Shopping)
*Refer to the latest `implementation_plan.artifact.md` for the technical breakdown of this UI/Logic split.*
