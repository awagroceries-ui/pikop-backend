# Implementation Plan - Advanced Feature Suite

This plan covers the next major evolution of the Pikop platform, focusing on user engagement, merchant growth, and operational efficiency.

## Proposed Changes

### 1. 🔗 Merchant Store Links (Direct-to-Shop)
- **Backend**:
    - [MODIFY] `vendors` and `kitchens` tables: Add `store_slug` (unique).
    - [MODIFY] `merchantController.js`: Auto-generate slug on profile setup and create `GET /api/v1/merchants/slug/:slug` to resolve storefront details.
- **Android**:
    - [MODIFY] `AndroidManifest.xml`: Register deep link scheme `pikop://store/{slug}`.
    - [MODIFY] `MerchantPortalScreen.kt`: Add a "Share My Store" card that generates the link using the system share sheet.
    - [MODIFY] `MainActivity.kt`: Handle the deep link and navigate directly to the `StorefrontScreen` with the resolved merchant ID.

### 2. 🎫 Merchant-Specific Promotions (Store Coupons)
- **Backend**:
    - [MODIFY] `coupons` table: Add `merchant_id` (nullable). If set, the coupon only applies to items from that specific merchant.
    - [MODIFY] `marketplaceController.js` & `kitchenController.js`: Update checkout validation to verify store-specific coupons.
- **Android**:
    - [MODIFY] `MerchantPortalScreen.kt`: Add a "Promotions" tab where merchants can create, toggle, and view usage stats for their own store coupons.

### 3. 🌓 Full Dark Mode Support (Premium UI Pass)
- **Android**:
    - [MODIFY] `Theme.kt`: Define a robust `darkColorScheme` using Pikop's secondary colors (Gold/Orange) as functional accents to ensure accessibility.
    - [REFACTOR] Global UI pass: Replace hardcoded `Color.White` or `Color.Black` in all feature screens with semantic theme colors (e.g., `MaterialTheme.colorScheme.surface`, `onSurface`, `primaryContainer`).

### 🤖 AI-Powered Support Assistant (Pikop Agent)
- **Backend**:
    - [NEW] `POST /api/v1/support/ask`: Connects user queries to Gemini 1.5 Flash. It will use a "RAG" (Retrieval-Augmented Generation) approach by passing the top 3 relevant FAQ articles as context to the AI.
- **Android**:
    - [MODIFY] `SupportHubScreen.kt`: Add a floating "Ask Pikop" chat bubble that opens an interactive AI assistant.

### 🛒 Multi-Item Shopping Cart
- **Android**:
    - [NEW] `CartManager`: A local Room database or DataStore to track `(productId, quantity, merchantId)`.
    - [MODIFY] Storefronts: Change "Buy Now" to "Add to Cart" and add a persistent Cart overlay.
- **Backend**:
    - [MODIFY] `commerceController.js`: Update `initializeCommerceOrder` to process an array of items and calculate the total weight/fare for the batch.

### 🗺️ In-App Route Rendering for Agents
- **Android**:
    - [MODIFY] `ActiveOrderScreen.kt`: Use the Google Maps Directions API to fetch and draw a `Polyline` representing the optimized path.

---

## User Review Required

> [!IMPORTANT]
> **Database Migration**
> Adding `store_slug` and `merchant_id` to coupons requires a schema update. I will handle this via a new migration file.

> [!NOTE]
> **Dark Mode Assets**
> I will use programmatic tinting for icons where possible. If any custom illustrations (like the Pikop logo) need dark-mode specific versions, I will flag them.

---

## Verification Plan

### Manual Verification
1.  **Deep Link Test**: Generate a store link as a Merchant. Click it from a WhatsApp/SMS message. Verify it opens the correct store in Pikop.
2.  **Dark Mode Toggle**: Switch system theme to Dark. Verify all screens (Checkout, Tracking, Wallet) are perfectly legible and brand-consistent.
3.  **Promo Gating**: Create a Merchant Coupon. Try using it on a different merchant's item. Verify it is rejected with "This coupon is only valid for [Store Name]".
4.  **AI Accuracy**: Ask the AI "What is the 10% fee?". Confirm it correctly references the "COD Platform Fee" from the FAQ.
