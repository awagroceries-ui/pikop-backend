# Implementation Plan - Nationwide-Ready Architecture

This plan generalizes the Pikop platform to support any Nigerian city/state via admin configuration, removing hardcoded references to launch cities (Port Harcourt, Lagos, Abuja).

## 🔍 Diagnostic Summary - Hardcoded Logic Found
1.  **Weather Service**: Hardcoded `CITIES` list in `weatherService.js`.
2.  **Onboarding**: Hardcoded "Port Harcourt" placeholder and Rider Permit tip in `SignupFulfillerScreen.kt`.
3.  **Discovery**: Hardcoded "Discover Port Harcourt" header in `StorefrontScreen.kt`.
4.  **API Defaults**: `CommerceOrderRequest` in `ApiService.kt` defaults city to "Port Harcourt".
5.  **Map Defaults**: Multiple screens default fallback coordinates to Lagos.
6.  **Email/Copy**: Hardcoded city lists in `notificationService.js` and `emailService.js`.

## Proposed Changes

### 1. Database & Schema

#### [NEW] [nationwide_readiness migration](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1726510000000_nationwide_readiness.js)
- **[NEW] `operating_cities` table**:
    - `id` (SERIAL PRIMARY KEY)
    - `name` (VARCHAR, unique) - e.g., "Port Harcourt"
    - `state_name` (VARCHAR) - e.g., "Rivers"
    - `lat`, `lng` (DECIMAL) - For weather and map biasing
    - `is_active` (BOOLEAN, default true)
    - `requires_rider_permit` (BOOLEAN, default false)
    - `daylight_start` (TIME, default '06:00')
    - `daylight_end` (TIME, default '18:00')
- **[NEW] `expansion_waitlist` table**:
    - `id` (SERIAL PRIMARY KEY)
    - `user_id` (INT, references users)
    - `city_name`, `email` (VARCHAR)
- **Seed**: Lagos, Abuja, Port Harcourt (with PH requiring rider permit).

### 2. Backend Logic (Node.js)

#### [MODIFY] [adminController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/adminController.js)
- Implement `getCities`, `addCity`, `updateCityRules` and `getExpansionWaitlist`.

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- `getQuote`: Check if `pickup_state` or `pickup_city` matches an active entry in `operating_cities`.
- Return `is_live: boolean` in the quote response.

#### [MODIFY] [weatherService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/weatherService.js)
- Replace hardcoded `CITIES` with a dynamic query from `operating_cities`.

#### [NEW] [expansionController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/expansionController.js)
- `joinWaitlist`: Endpoint for users to request Pikop in their city.

### 3. Android App (Compose)

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Remove hardcoded default values for city.
- Add `is_live` to `QuoteResponse`.
- Add `joinWaitlist` endpoint.

#### [MODIFY] [SignupFulfillerScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/SignupFulfillerScreen.kt)
- Fetch active cities and rules on init.
- Dynamically show "Commercial Rider Permit" requirement based on the selected city's rules.

#### [MODIFY] [StorefrontScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/commerce/StorefrontScreen.kt)
- Change header to "Discover [Current City]" or "Discover Nearby".
- If no items found and city is not live, show a "Coming Soon" card with a "Notify Me" button.

#### [MODIFY] [OrderQuoteScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/OrderQuoteScreen.kt)
- If `is_live` is false, block deployment and show the Waitlist dialog.

## Verification Plan

### Manual Verification
1.  **Add City**: Add a new city (e.g., "Uyo") via the Admin Dashboard. Verify it appears in the app and allows orders.
2.  **Toggle Inactive**: Deactivate a city in Admin. Verify the app shows the "Coming Soon" / Waitlist UI.
3.  **Permit Check**: Select Port Harcourt in Fulfiller signup; verify permit text shows. Select Lagos; verify it doesn't (unless configured).
4.  **Nationwide Signup**: Sign up with an address in a non-launch state (e.g., Kano). Verify account creation works, but "Request Delivery" is gated.
