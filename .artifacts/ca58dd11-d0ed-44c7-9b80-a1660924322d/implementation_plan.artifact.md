# Implementation Plan - Milestone 2: Fulfiller 2.0 & Advanced Onboarding

Upgrade the fulfiller experience with automated KYC, live identity capture, class-conditional onboarding flows, and formalized public profile cards.

## User Review Required

> [!IMPORTANT]
> - **Branching Onboarding**: Agents (including Cyclists) will now skip vehicle-related documents entirely. Riders and Drivers will be required to provide vehicle registration numbers and specific licenses.
> - **Mandatory Live Photo**: I will implement a mandatory **Profile Photo** step. To prevent fraud, the app will force a live camera capture and block gallery uploads.
> - **Cyclist Mobility**: Agents can now select `bicycle` as a mobility type, which will widen their dispatch radius to match Riders.
> - **Public Identity**: Users will now see a verified Fulfiller Profile (name, live photo, tier, and vehicle plate number) when matched.

## Proposed Changes

### 1. Schema & Database (Backend)

#### [NEW] `backend/migrations/1723010000000_fulfiller_2_0.js`
- **Fulfillers**:
    - Add `mobility_type` (on_foot, public_transit, bicycle).
    - Add `profile_photo_url` (varchar, nullable).
    - Add `tier` (bronze, silver, gold, default: bronze).
- **Vehicles**: Create `vehicles` table (id, fulfiller_id, registration_number, make, model, color).

---

### 2. Class-Conditional Logic (Backend)

#### [MODIFY] `fulfillerController.js`
- **`updateProfile`**: New endpoint to handle branching data (mobility_type for Agents, vehicle details for Riders/Drivers, and the mandatory profile photo).
- **`submitApplication`**: Validates that the **Live Profile Photo** and all required documents for the specific class are present before allowing submission.
- **Dispatch radius**: Update `findNearbyFulfillers` to use a multiplier for `bicycle` mobility (configurable in settings).

---

### 3. Fulfiller Public Profile (Backend & Android)

#### [MODIFY] `orderController.js`
- Create a shared `getPublicProfile(fulfillerId)` helper returning only the safe subset (Name, Photo, Tier, Plate).
- Inject this profile into all Order and Offer responses.

#### [MODIFY] `ActiveOrderScreen.kt` (User App)
- Update the "Driver Assigned" card to show:
    - **Live Profile Photo** & Name.
    - Tier Badge (Bronze/Silver/Gold).
    - **Vehicle Registration Number** (if Rider/Driver).
    - Star Rating.

---

### 4. Advanced Onboarding Flow (Android)

#### [MODIFY] `KycUploadScreen.kt`
- Implement flow branching:
    - **Identity Step**: Didit KYC (Shared).
    - **Live Photo Step**: Force camera capture for the fulfiller's face.
    - **Branch 1 (Agent)**: Skip vehicle docs -> Mobility Type Selection.
    - **Branch 2 (Rider/Driver)**: Vehicle Details -> Vehicle Documents.

---

## Verification Plan

### Manual Verification
1.  **Agent Flow**: Sign up as an Agent, complete Didit, take a live photo, and verify the app asks for Mobility Type but NOT a license.
2.  **Rider Flow**: Sign up as a Rider and verify the app requires a Live Photo, License, and Vehicle Plate before submission.
3.  **Photo Lockdown**: Attempt to upload a profile photo from the gallery; verify that only the camera option is available.
4.  **Profile Visibility**: Match an order with a Driver and verify the User app correctly displays the Driver's **Live Photo** and plate number.
