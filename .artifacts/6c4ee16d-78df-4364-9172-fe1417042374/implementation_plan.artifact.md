# Implementation Plan - Business/Corporate Accounts

This plan introduces the **Corporate Account** type, enabling businesses to manage and pay for deliveries on behalf of employees or clients using a centralized wallet and spending limits.

## User Review Required

> [!IMPORTANT]
> **Billing Strategy Selection**
> As recommended, I am starting with **(a) Centralized Wallet** billing. The company tops up their corporate wallet, and authorized users draw from it. **Post-paid Invoicing** is scoped for a future phase to avoid credit-risk complexity.

> [!NOTE]
> **Role Mapping**
> Authorized employees/clients will keep their individual account roles (e.g., CUSTOMER) but will be linked to a Corporate Account via a junction table. They will see "Corporate Billing" as an option at checkout.

## Proposed Changes

### 1. Database & Schema Enhancements

#### [NEW] [corporate_infrastructure migration](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1726540000000_corporate_infrastructure.js)
- **Extend `users` table**: Add `CORPORATE` to the role check constraint.
- **[NEW] `corporate_accounts` table**:
    - `id` (UUID PRIMARY KEY)
    - `user_id` (INT, owner/admin)
    - `company_name` (VARCHAR)
    - `cac_number` (VARCHAR, unique)
    - `billing_email` (VARCHAR)
    - `billing_type` (VARCHAR, default 'prepaid_wallet')
    - `status` (VARCHAR, e.g., 'PENDING', 'ACTIVE')
- **[NEW] `corporate_sub_accounts` table**: (Junction for authorized users)
    - `corporate_account_id` (UUID)
    - `user_id` (INT)
    - `role` (VARCHAR, e.g., 'STAFF', 'ADMIN')
    - `daily_spend_limit` (DECIMAL)
    - `monthly_spend_limit` (DECIMAL)
- **Extend `orders` & `wallets`**: Add `corporate_account_id` foreign keys.

### 2. Backend Logic (Node.js)

#### [NEW] [corporateController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/corporateController.js)
- `setupCorporateProfile`: Handle B2B verification.
- `addStaffMember`: Invite users by email to join the corporate account.
- `updateStaffLimit`: Manage per-user daily/monthly caps.
- `getCorporateDashboard`: Reporting on spend by user and date.

#### [MODIFY] [walletService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/walletService.js)
- Implement `processCorporateDebit(orderId, corporateAccountId)`:
    - Verify sufficient corporate wallet balance.
    - Check if the user has exceeded their daily/monthly limit before allowing the debit.

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- Update `createOrder` to check for `corporate_account_id` in the request.
- Trigger `processCorporateDebit` if corporate billing is selected.

### 3. Android App Integration

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Standardize corporate DTOs and endpoints.

#### [MODIFY] [CorporateDashboardScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/CorporateDashboardScreen.kt)
- Implement the "Staff Management" view with limit editing.
- Add spend summary charts (if feasible) or simple stats.

#### [MODIFY] [OrderQuoteScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/order/OrderQuoteScreen.kt)
- Ensure the "Billing Method" selector correctly passes the `corporate_account_id` to the backend.

## Verification Plan

### Manual Verification
1.  **Onboarding**: Register a Corporate account. Approve via Admin.
2.  **Staff Linking**: Add an employee's email. Log in as that employee and verify "Corporate Billing" appears in checkout.
3.  **Spend Enforcement**: Set a ₦1,000 daily limit. Attempt an order for ₦1,500. Verify it is blocked.
4.  **Reporting**: Place multiple orders. Check the Corporate Dashboard and verify stats are aggregated correctly by user.
