# Implementation Plan - COD Parity for Fulfiller App & Admin Dashboard

This plan extends the COD/Escrow system to the fulfiller app and admin dashboard, ensuring consistency and clear visibility across all roles.

## User Review Required

> [!IMPORTANT]
> **Fulfiller Role:** Fulfillers will now explicitly see when a mission is "Delivery + COD". They are informed that payment is already escrowed and they should **never** collect cash.
>
> **Admin Arbitration:** The dashboard now includes a "Disputes" section where admins can review buyer claims and choose to either `REFUND` to the buyer or `RELEASE` to the seller.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [adminController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/adminController.js)
- **`getOrders`**: Add `order_type` and `escrow_status` to the list view. Add filtering support for COD missions.
- **`trackOrder`**: Enrich the mission detail view with:
    - Escrow status history.
    - Fee breakdown (Item price, platform fee, delivery fee).
    - Ledger trail (Wallet entries related to this mission).
- **`getFulfillerDetail` [NEW]**: View individual fulfiller stats and wallet breakdown (`pending` vs `available`).

#### [MODIFY] [adminRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/adminRoutes.js)
- Register the new `getFulfillerDetail` route.

#### [MODIFY] [orders.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/orders.ejs)
- Update the Mission Board to show "COD" badges and `fee_payer` info.

#### [MODIFY] [admin_track.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/admin_track.ejs)
- Add a "Financial Audit" section showing the escrow ledger.

---

### Android App

#### [MODIFY] [ActiveOrderScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/ActiveOrderScreen.kt)
- **Mission Indicator:** Display a prominent **"Delivery + COD (Escrow Protected)"** badge if `item_price > 0`.
- **Status Support:** Explicitly handle `DELIVERED_PENDING_CONFIRMATION` status by showing a "Waiting for Customer Confirmation" state.
- **Cash Guard:** Verify no "Mark as Paid" or manual collection buttons exist for escrowed missions.

#### [MODIFY] [IncomingOfferComponent.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/IncomingOfferComponent.kt)
- (Already partially done) Ensure "Delivery + COD" label is prominent and consistent with `ActiveOrderScreen`.

---

## Verification Plan

### Automated Tests
- Syntax check backend: `node -c src/controllers/adminController.js`.
- Build Android app: `./gradlew assembleDebug`.

### Manual Verification
1.  **Fulfiller View:** Start a COD mission. Verify the "Escrow Protected" badge appears and no cash collection options are shown.
2.  **Delivery Flow:** Complete delivery and verify the fulfiller app enters the "Waiting for Confirmation" state.
3.  **Admin Dashboard:** Locate the mission in the admin board. Verify the fee breakdown and ledger entries are visible and correct.
4.  **Dispute Test:** File a dispute as a customer. Verify it appears in the admin dashboard and can be resolved.
