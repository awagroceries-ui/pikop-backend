# Implementation Plan: Fulfiller Category Split

## 🔍 Diagnostic Report

I have investigated the backend and current frontend setups:
1. **Category Mapping**: The backend uses `primary_class` and `mobility_type` fields inside the `fulfillers` table (e.g. `RIDER`, `DRIVER`).
2. **KYC & Documents**: Prembly handles identity verification. `uploadDocument` in `fulfillerController.js` handles arbitrary documents (like licenses or permits) by inserting into a generic `kyc_documents` table for manual admin review. This confirms **vehicle documents are separate from the core Prembly identity flow** and are handled via standard file uploads.
3. **Public Profile Separation**: Currently, `updateFulfillerProfile` writes `date_of_birth` and `home_address` into the `fulfillers` table. Public access to this data (e.g., in a customer tracker view) will just need to make sure the DTO/response object excludes those verification-only fields.

## User Review Required

> [!IMPORTANT]
> **Sub-category Routing Design**
> I will replace the single `SignupFulfillerScreen.kt` with a two-step flow:
> 1. `FulfillerCategorySelectionScreen.kt` - Prompts the user to pick "Foot Agent / Cyclist", "Rider (Motorcycle)", or "Driver (Car/Van)".
> 2. `SignupFulfillerScreen.kt` - The actual form. It will take a `category` argument in its navigation route (e.g., `signup_fulfiller/rider`) and dynamically render the correct fields, rather than maintaining 3 separate identical forms for basic info. It will hide vehicle fields for Cyclists, and show the commercial permit option specifically for Riders.
>
> Do you approve this architecture?

## Proposed Changes

### Part 1: Category Selection Step

#### [NEW] [FulfillerCategorySelectionScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/FulfillerCategorySelectionScreen.kt)
- A screen immediately following `UserTypeSelectionScreen` (if "Fulfiller" is chosen).
- Presents 3 cards: `FOOT_AGENT`, `RIDER`, `DRIVER`.
- Navigates to `signup_fulfiller/{category}` upon selection.

### Part 2: Dynamic Fulfiller Signup Form

#### [MODIFY] [SignupFulfillerScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/SignupFulfillerScreen.kt)
- Accept `category` as a parameter.
- **Common Fields**: Full Name, Email, Phone, Password, DOB, Home Address, Gender.
- **Vehicle Fields (Only if RIDER or DRIVER)**: Plate Number, Brand, Model, Color.
- **Document Fields**: Add UI placeholders/logic for ID upload (all), License upload (Rider/Driver), Insurance (Driver), and Commercial Permit (Rider only, marked optional, showing a Port Harcourt tip).
- Submit via API by extending the payload to include these extra fields.

### Part 3: Navigation Routing

#### [MODIFY] [MainActivity.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/MainActivity.kt)
- Add route: `composable("fulfiller_category_selection")`.
- Update the fulfiller route: `composable("signup_fulfiller/{category}")`.
- Update `UserTypeSelectionScreen` to navigate to `fulfiller_category_selection` when `FULFILLER` is chosen.

## Verification Plan

### Manual Verification
1. Launch app -> Tap "I want to Earn" -> Verify it shows 3 mobility categories.
2. Select "Foot Agent / Cyclist" -> Verify no vehicle details or driver's license fields appear.
3. Select "Rider" -> Verify vehicle fields appear, plus license and the optional Commercial Permit.
4. Verify form submission works even if the optional commercial permit is left blank.