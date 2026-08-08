# Milestone 20: Order Lifecycle & Security Enhancements

## Phase 1: Database & Schema
- [x] Create `order_lifecycle_ext` migration
- [x] Extend Dispute categories in database

## Phase 2: Pre-Acceptance Logic (Backend)
- [x] Update `createOrder` to require `item_photo_url` and `pickup_display_summary`
- [x] Update `getOffers` to include preview data
- [x] Update `verifyDelivery` to require `delivery_photo_url`

## Phase 3: Cancellation & Incident Flow (Backend)
- [x] Implement 25% non-free cancellation logic in `orderController.js`
- [x] Implement `processCancellationFee` in `walletService.js`
- [x] Implement `POST /api/v1/orders/:orderId/incident` endpoint
- [x] Implement `HANDOFF` and `CANCEL_WITH_WAIVER` resolution logic

## Phase 4: Android UI Integration
- [x] `OrderQuoteScreen.kt`: Add item photo capture/upload
- [x] `IncomingOfferComponent.kt`: Display item photo and display summary
- [x] `ActiveOrderScreen.kt` & `TrackOrderScreen.kt`: Add "Cancel/Incident" UI
- [x] Add `delivery_photo_url` upload to completion flow

## Phase 5: Admin Refinement
- [x] Update Disputes view to handle incident categories and fee waivers
