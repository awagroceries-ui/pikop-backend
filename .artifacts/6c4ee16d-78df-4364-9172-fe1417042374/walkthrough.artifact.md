# Walkthrough - Group-Specific FAQ Integration

I have successfully restructured the Help Center to be group-aware and category-organized, ensuring users only see the content relevant to their specific role (Customer, Fulfiller, or Merchant).

## Changes Made

### 📚 1. Structured Knowledge Base (Backend)
- **Data Migration**: I created a new migration `1726570000000_seed_structured_faqs.js` that seeds the entire provided `Pikop_FAQs_Content.md` into the database. This replaces the old, flat list with structured group/category mapping.
- **Audience Constraints**: Updated the database schema to explicitly support the `MERCHANT` and `CORPORATE` target audiences.
- **Dynamic Filtering**: Updated the `supportController.js` to automatically filter articles based on the authenticated user's actual role.

### 📱 2. Redesigned Support Hub (Android UI)
- **Role-Aware Defaulting**: When a user opens the Help Center, the app now automatically selects the correct help group (e.g., an Agent sees Fulfiller FAQs by default).
- **Role Switcher**: For users with multiple capabilities (like a Customer who is also a Merchant), I added a tab-style switcher to flip between "Customer Help" and "Seller/Agent Help."
- **Search Bar**: Added a global search field at the top. Users can now search both question and answer text across all categories in their section.
- **Accordion Navigation**: Categories are now organized into collapsible groups. This keeps the large volume of content navigable without overwhelming the user.
- **Text Visibility**: Confirmed that long FAQ answers (like the detailed COD breakdown) are fully scrollable and perfectly visible, carrying over the fix from previous iterations.

### 🧹 3. Code Cleanup
- **Simplified Routing**: Removed the redundant `FaqListScreen.kt` and consolidated the UI into a more modern, single-screen hub with nested expansion.
- **Standardized DTOs**: Updated `ApiService.kt` to support the group-based querying.

## Verification Results
- **Customer Role**: [VERIFIED] Only Customer-relevant categories (Dispatch, Food, Groceries, etc.) are shown by default.
- **Fulfiller Role**: [VERIFIED] Shows Agent-specific info like Streak Bonuses and Payout details.
- **Merchant Role**: [VERIFIED] Displays Seller-centric info on Commissions and Operating Hours.
- **Search Logic**: [VERIFIED] Typing "wallet" correctly filters the list to only relevant questions.
- **Build Status**: [SUCCESS] Successfully compiled the Android app.

## Deployment Instructions
To push the new FAQ content live to your production server:
```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
npm run migrate:up
pm2 restart pikop-v3
```
