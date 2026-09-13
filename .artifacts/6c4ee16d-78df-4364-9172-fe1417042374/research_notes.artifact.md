# Research Notes - Marketplace & Onboarding Audit

I have audited the current implementation of the Marketplace and Onboarding flows to identify discrepancies with the new target architecture specification.

## 1. Marketplace Structure Audit
- **Current Entry Points**: Primary entry is via the "Shop & Eat" bottom navigation tab.
- **Current Storefront**: A single `StorefrontScreen` that fetches all items (products and meals) via a unified `getDiscovery` API.
- **Missing Modules**: No dedicated entries for **Dispatch**, **Food & Kitchen**, **Groceries**, and **Marketplace** on the main Home screen.
- **Home Screen Layout**: Dominated by a large "Request a Delivery" card, missing the specified 2x2 grid or horizontal scroll for primary modules.

## 2. Onboarding Flow Audit
- **Role Selection**: `UserTypeSelectionScreen` only displays "CUSTOMER" and "FULFILLER". The "MERCHANT" role is missing from the initial entry.
- **Onboarding Branching**: Signup currently funnels all roles through the same form (`SignupScreen`), with role-specific logic only appearing during the post-OTP verification phase (e.g., `KycUploadScreen`).
- **Fulfiller Sub-types**: `KycUploadScreen` handles sub-types (Foot, Rider, Driver) through a single multi-step form with conditional visibility in later steps. It lacks immediate branching at the start.
- **Merchant Verification**: `MerchantRegistrationScreen` is a single business info form. It does not separate "Contact Person Verification" from "Business Verification".

## 3. Merchant Portal Audit
- **Integration**: The Merchant Portal lives inside the main app navigation stack and is accessible via the Account menu.
- **Functionality**: Basic dashboard and listing management are present, but it lacks the "standalone experience" branding and strictly defined order-handoff boundaries.

## 4. Backend/Data Audit
- **Discovery API**: Already supports category filtering, which can be leveraged for the 4 separate modules.
- **Tables**: `vendors` and `kitchens` are already separated at the database level, providing a solid foundation for the split.
- **Order Linking**: Recent migrations have added links for `product_id` and `menu_item_id` to the `orders` table.

## Conclusion
The technical foundation (database and basic screens) is ready, but the **Information Architecture** and **User Flows** need a significant overhaul to match the specified professional standards.
