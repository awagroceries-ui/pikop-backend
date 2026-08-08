# Implementation Plan - Order Lifecycle Enhancements

Implement item-photo preview, masked-location previews, no-free-cancellation policy, and an Incident Report flow to improve trust, security, and operational efficiency.

## User Review Required

> [!IMPORTANT]
> - **Schema Migration**: I will add `item_photo_url` and `delivery_photo_url` to the `orders` table. `delivery_photo_url` will now be mandatory for completing a delivery.
> - **Privacy**: Fulfillers will only see the `display_summary` (e.g., "Near UNIPORT Gate") before acceptance. Exact coordinates and addresses are revealed only after they accept.
> - **Instant Fees**: Cancellations after `MATCHED` will immediately charge 25% of the fare. Waivers can be requested via the Incident flow and approved later by Ops.

## Proposed Changes

### 1. Database & Schema (Backend)

#### [NEW] `backend/migrations/1722950000000_order_lifecycle_ext.js`
- **Orders**: Add `item_photo_url` (nullable/required by logic), `delivery_photo_url` (required at delivery), `cancellation_fee_waived` (bool, default false), `incident_dispute_id` (uuid, FK).
- **Disputes**: Extend category enum and resolution options.
- **Addresses**: (Since addresses are currently strings in the `orders` table, I will add `pickup_display_summary` and `delivery_display_summary` directly to the `orders` table for simplicity and alignment with the current schema).

---

### 2. Pre-Acceptance Preview (Full Stack)

#### [MODIFY] `OrderQuoteScreen.kt` (Android)
- Add a required "Take Photo of Item" step before the final request.
- Upload photo to `/uploads/items/` via a new endpoint.

#### [MODIFY] `orderController.js` (Backend)
- Update `getQuote`/`createOrder` to handle the `item_photo_url`.
- Update `getOffers` to include `item_photo_url` and `pickup_display_summary`.

#### [MODIFY] `IncomingOfferComponent.kt` (Android)
- Display the item image and the masked pickup summary.

---

### 3. Cancellation Policy & Incident Flow (Backend)

#### [MODIFY] `orderController.js`
- **User Cancellation**: Block free cancellation after `MATCHED`. Charge 25% fee (100% to platform wallet).
- **New Endpoint**: `POST /orders/:id/incident`
    - Logic for `HANDOFF`: Reset order to `SEARCHING`, keep original incident attached, show special message to user.
    - Logic for `CANCEL_WITH_WAIVER`: Charge fee immediately, create Dispute.
    - `security_risk` triggers auto-waiver.

#### [MODIFY] `walletService.js`
- Implement `processCancellationFee(orderId)` to handle the 25% platform credit.

---

### 4. Admin Dashboard (UI)

#### [MODIFY] `disputes.ejs`
- Add filters for incident categories.
- Add "Waive Fee" / "Deny Waiver" buttons.

## Verification Plan

### Manual Verification
1.  **Offer Preview**: As a Fulfiller, verify you can see the item photo and "Near [Landmark]" summary before accepting.
2.  **User Cancellation**: As a Customer, cancel a matched order and verify 25% is deducted from the wallet.
3.  **Incident Flow**: As a Fulfiller, file a `breakdown` incident with `handoff`. Verify the order returns to the search pool for other drivers.
4.  **Auto-Waiver**: File a `security_risk` incident and verify the cancellation fee is waived immediately.
