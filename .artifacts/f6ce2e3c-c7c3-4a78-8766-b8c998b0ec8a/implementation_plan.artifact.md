# Implementation Plan - Mission Preview, Fulfiller Profile & Onboarding Gaps

This plan addresses missing data collection in fulfiller onboarding, adds a mission preview for fulfillers, and surfaces a detailed fulfiller profile to customers.

## User Review Required

> [!IMPORTANT]
> **Verification Tier Logic:** A performance-based scoring system (Basic/Standard/Elite/Super) does not yet exist in the codebase. As requested, I will flag this as a separate, larger task. For now, I will add the `tier` column to the database defaulting to 'Basic' and surface it in the UI.
>
> **Missing Onboarding Fields:** I have identified that `address`, `date_of_birth`, and `gender` are currently missing from the fulfiller onboarding flow and database schema. I will add these.

## Proposed Changes

### Backend (`backend_v3`)

#### [NEW] [Migration](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1725587000000_add_extended_fulfiller_profile_fields.js)
- Add `gender`, `date_of_birth` (date), `home_address` (text), `tier` (varchar, default 'Basic'), and `rating_count` (int, default 0) to the `fulfillers` table.

#### [MODIFY] [fulfillerController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/fulfillerController.js)
- **`updateFulfillerProfile`**: Add support for `gender`, `date_of_birth`, and `home_address`.
- **`getAvailableOffers`**:
    - Include `collect_on_delivery_amount` in the response.
    - Calculate and return `distance_km` using `ST_Distance`.
    - Determine `mission_type`: "Delivery only" or "Delivery + COD collection".

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- **`getOrderDetails`**: Select more fulfiller fields (`profile_photo_url`, `tier`, `registration_number`, `rating_avg`, `rating_count`, `primary_class`, `mobility_type`) to support the customer-side profile view.
- **`rateFulfiller`**: Update `rating_count` in the `fulfillers` table when a new rating is submitted.

---

### Android App

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Update `ProfileUpdateRequest`, `FulfillerProfileResponse`, `FulfillerPublicProfile`, and `OfferResponse` with the new fields.

#### [MODIFY] [KycUploadScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/KycUploadScreen.kt)
- Add a new onboarding step (e.g., "Personal Details") to collect `gender` (dropdown), `date_of_birth` (date picker), and `home_address`.

#### [MODIFY] [IncomingOfferComponent.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/IncomingOfferComponent.kt)
- Update the UI to display:
    - **Mission Type:** (e.g., "Delivery + COD") with a distinct icon/color.
    - **Est. Distance:** in KM.
    - **Fare:** prominent display.

#### [NEW] [FulfillerProfileDialog.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/fulfiller/FulfillerProfileDialog.kt)
- Create a reusable dialog component to show the fulfiller's public profile (Photo, Name, Category, Vehicle, Rating, Tier).

#### [MODIFY] [TrackOrderScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/TrackOrderScreen.kt)
- Make the `FulfillerCard` clickable to launch the `FulfillerProfileDialog`.

---

## Verification Plan

### Automated Tests
- Syntax check backend: `node -c ...`.
- Build Android app: `./gradlew assembleDebug`.

### Manual Verification
1.  **Fulfiller Onboarding:** Create a new fulfiller account and verify the new "Personal Details" step works.
2.  **Mission Preview:** As a fulfiller, receive an offer and verify "Delivery + COD" and "Distance" are visible before accepting.
3.  **Customer View:** As a customer, tap the assigned agent's card and verify the full profile (including Tier and Rating Count) is displayed correctly.
