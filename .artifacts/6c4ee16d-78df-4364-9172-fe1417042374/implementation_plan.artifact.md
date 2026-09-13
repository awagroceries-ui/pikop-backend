# Implementation Plan: Distinct Onboarding per User Type

## 🔍 Diagnostic Report (Current Reality)

I have investigated the current signup structure and backend data model:
1. **Upfront Selection**: `UserTypeSelectionScreen.kt` currently exists but only offers two choices: "I want to Send" (Customer) and "I want to Earn" (Fulfiller). It is entirely missing the Merchant role.
2. **Monolithic Signup Form**: Both existing roles are routed to a single, generic `SignupScreen.kt` component (`signup/{role}` route). The UI uses shared state and basic conditional logic (e.g., `if (isFulfiller) "Join the Fleet" else "Create Pikop Account"`) to tweak text, rather than genuinely distinct flows.
3. **Backend Support**: The backend `users` table and `authController.js` already support a generic `role` parameter. The signup endpoint simply takes `role` from `req.body` and stores it. The core table supports `MERCHANT` natively. No deep schema changes are needed just for initial account creation, although specific merchant/fulfiller tables will likely be involved in subsequent detailed prompts.

## User Review Required

> [!IMPORTANT]
> **Splitting the Monolith**
> I will replace the single `SignupScreen.kt` with three entirely separate files:
> - `SignupCustomerScreen.kt` (Baseline, lightweight)
> - `SignupFulfillerScreen.kt` (Stubbed out/separated for now, ready for the Fulfiller prompt)
> - `SignupMerchantScreen.kt` (Stubbed out/separated for now, ready for the Merchant prompt)
>
> The routes in `MainActivity.kt` will be updated to point to these explicit destinations instead of the generic `signup/{role}` route. Do you approve of this strict separation?

## Proposed Changes

### Part 1: Upfront Role Selection

#### [MODIFY] [UserTypeSelectionScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/UserTypeSelectionScreen.kt)
- Add a third `RoleCard` for **Merchant** ("I want to Sell" / "List products & grow your business").
- Update icons: `ShoppingBag` for Customer (or similar), `ElectricBike` for Fulfiller, `Storefront` for Merchant.
- Update routing logic to emit the specific role strings: `CUSTOMER`, `FULFILLER`, `MERCHANT`.

### Part 2: Separate Onboarding Flows

#### [DELETE] [SignupScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/SignupScreen.kt)
- Remove the monolithic shared screen.

#### [NEW] [SignupCustomerScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/SignupCustomerScreen.kt)
- Lightweight onboarding (Full Name, Email, Phone, Password). No business fields. Hardcodes the role request as `CUSTOMER`.

#### [NEW] [SignupFulfillerScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/SignupFulfillerScreen.kt)
- A decoupled flow specifically for Fulfillers. For this foundational prompt, it will contain basic fields but will hardcode the request as `FULFILLER`, providing a clean slate for the Fulfiller categorization prompt.

#### [NEW] [SignupMerchantScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/SignupMerchantScreen.kt)
- A decoupled flow for Merchants. Will act as a stub with basic fields and hardcode `MERCHANT` for now, ready for the detailed Merchant onboarding prompt.

### Part 3: Navigation Routing

#### [MODIFY] [MainActivity.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/MainActivity.kt)
- Remove `composable("signup/{role}")`.
- Add distinct routes: `composable("signup_customer")`, `composable("signup_fulfiller")`, `composable("signup_merchant")`.
- Map the `UserTypeSelectionScreen` callbacks to these new routes.

## Verification Plan

### Automated/Code Verification
- Verify `MainActivity.kt` routes successfully to the 3 independent modules.

### Manual Verification
1. Launch the app to the `UserTypeSelectionScreen`.
2. Verify 3 distinct options are visible (Customer, Fulfiller, Merchant).
3. Tap Customer -> Verify it goes to the Customer signup screen.
4. Go back, tap Fulfiller -> Verify it goes to the Fulfiller signup screen.
5. Go back, tap Merchant -> Verify it goes to the Merchant signup screen.
6. Verify no shared conditional logic remains bridging these flows.