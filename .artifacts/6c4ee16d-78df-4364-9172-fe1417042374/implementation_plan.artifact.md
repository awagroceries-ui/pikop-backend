# Implementation Plan - Fleet Partner Program

This plan introduces the **Fleet Partner** program, allowing logistics companies to manage their fleets on Pikop with custom commission rates and priority routing.

## Proposed Changes

### 1. Database Schema Enhancements

#### [NEW] [fleet_partner_program migration](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1726500000000_fleet_partner_program.js)
- **Extend `users` table**: Add `FLEET_PARTNER` to the role check constraint.
- **[NEW] `fleet_partners` table**:
    - `id` (SERIAL PRIMARY KEY)
    - `user_id` (INT, references `users`)
    - `business_name` (VARCHAR)
    - `cac_number` (VARCHAR, unique)
    - `address` (TEXT)
    - `fleet_size` (INT)
    - `vehicle_types` (VARCHAR[])
    - `cities` (VARCHAR[])
    - `commission_override` (DECIMAL) - Custom share for Pikop (default 25%)
    - `has_overflow_priority` (BOOLEAN, default false)
    - `status` (VARCHAR, e.g., 'PENDING', 'VERIFIED')
- **[NEW] `fleet_partner_invites` table**:
    - `id` (SERIAL PRIMARY KEY)
    - `fleet_partner_id` (INT, references `fleet_partners`)
    - `invite_code` (VARCHAR, unique)
    - `is_active` (BOOLEAN, default true)
- **Extend `fulfillers` table**: Add `fleet_partner_id` (INT, references `fleet_partners`).

### 2. Backend Logic (Node.js)

#### [NEW] [fleetPartnerController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/fleetPartnerController.js)
- `setupFleetProfile`: Initial B2B application/onboarding.
- `getFleetDashboard`: Aggregates linked fulfiller status and earnings.
- `generateInviteCode`: For partners to onboard their drivers.

#### [MODIFY] [authController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/authController.js)
- Update `signup` to handle `fleet_invite_code` for Fulfillers, linking them to a partner.

#### [MODIFY] [dispatchService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/dispatchService.js)
- Implement **Overflow-based Priority**:
    - Independent Fulfillers get the offer first.
    - If unaccepted after 3 minutes, broadcast to Fleet Partners with `has_overflow_priority`.

#### [MODIFY] [walletService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/walletService.js)
- Update `processMissionSettlement` to check if a Fulfiller is linked to a Fleet Partner and apply their `commission_override` if it exists.

### 3. Admin Integration

#### [MODIFY] [adminController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/adminController.js)
- Add management of Fleet Partners: Verify business, set custom commission, and toggle overflow priority.

### 4. Android App (Compose)

#### [MODIFY] [UserTypeSelectionScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/UserTypeSelectionScreen.kt)
- Add "Logistics Partner" card for B2B signup.

#### [NEW] `FleetPartnerOnboarding.kt`
- Step-by-step business info and fleet details collection.

#### [NEW] `FleetPartnerDashboard.kt`
- Management view for fleet owners.

#### [MODIFY] [SignupFulfillerScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/SignupFulfillerScreen.kt)
- Add "Invite Code (Optional)" field for drivers onboarding under a fleet.

## User Review Required

> [!IMPORTANT]
> **Priority Routing Logic**
> The overflow window (3 minutes) is currently implemented in `dispatchReminderJob.js`. I will update this job to check for fleet priority eligibility upon re-broadcast.

> [!CAUTION]
> **Commission Payouts**
> For fleet-linked Fulfillers, the payout still goes to the *individual Fulfiller's wallet*. The Fleet Partner manages the aggregate view but doesn't centrally receive the drivers' earnings unless a central billing mandate is built later.

## Verification Plan

### Manual Verification
1.  **Fleet Onboarding**: Apply as a Fleet Partner, approve via Admin, and generate an invite code.
2.  **Linked Fulfiller**: Onboard a Rider using the invite code. Verify the `fleet_partner_id` is set.
3.  **Commission Test**: Complete a mission with a linked Rider. Verify the custom commission rate (e.g., 15% instead of 25%) is applied.
4.  **Overflow Test**: Create an order. Wait 3 minutes without accepting as an independent. Verify Fleet-linked Riders get the offer only then.
