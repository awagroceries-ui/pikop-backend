# Implementation Plan - Milestone 3: Corporate/SME Infrastructure

Introduce high-value Corporate and SME account management, enabling multi-user billing, real-time Paystack mandates, and automated monthly itemized invoicing.

## User Review Required

> [!IMPORTANT]
> - **Unified Wallet System**: I will extend the existing `wallets` table to support a new `owner_type = 'CORPORATE'`. This ensures a single source of truth for all financial transactions.
> - **Real-Time Billing**: Even for "Monthly Invoice" accounts (Direct Debit), payments are captured in real-time per order. The invoice acts as a consolidated record-keeping document.
> - **Sub-Account Mapping**: A single Pikop User can be linked to multiple Corporate accounts (e.g., as staff for one and billing admin for another).

## Proposed Changes

### 1. Database & Schema (Backend)

#### [NEW] `backend/migrations/1723020000000_corporate_infrastructure.js`
- **`corporate_accounts`**: `id` (uuid), `company_name`, `billing_email`, `billing_type` (direct_debit, prepaid_wallet), `paystack_mandate_id`, `status` (pending, active, suspended).
- **`corporate_sub_accounts`**: `id` (uuid), `corporate_account_id` (FK), `user_id` (FK), `role` (staff, billing_admin).
- **`orders`**: Add `corporate_account_id` (uuid, FK).
- **`wallets`**: Add `corporate_account_id` (uuid, FK) and extend `owner_type` to include `'CORPORATE'`.

---

### 2. Corporate Logic & Onboarding (Backend)

#### [NEW] `backend/src/controllers/corporateController.js`
- **`createAccount`**: Initializes a corporate profile and links the requesting user as `billing_admin`.
- **`authorizeMandate`**: Generates a Paystack authorization URL for recurring direct debit.
- **`addStaff`**: Allows billing admins to invite staff members via email.
- **`getInvoices`**: Retrieves generated monthly PDF summaries.

#### [MODIFY] `backend/src/routes/corporateRoutes.js`
- Register corporate management endpoints.

---

### 3. Payment & Invoice Engine (Backend)

#### [MODIFY] `backend/src/controllers/orderController.js` & `walletService.js`
- Detect if a user is ordering on behalf of a Corporate account.
- **Prepaid**: Debit the corporate wallet instantly. Block if insufficient funds.
- **Direct Debit**: Trigger a real-time charge against the `paystack_mandate_id`.

#### [NEW] `backend/src/jobs/monthlyInvoice.js`
- A scheduled script using `pdfkit` to generate itemized monthly summaries for all active corporate accounts.
- Automatically emails the PDF to the registered `billing_email`.

---

### 4. Admin Command Center

#### [NEW] `backend/src/views/corporate_accounts.ejs`
- A dedicated management screen for Ops to monitor corporate accounts, verify mandates, and suspend accounts if necessary.

---

## Verification Plan

### Manual Verification
1.  **Corporate Signup**: Create a corporate account and verify the `billing_admin` sub-account is created.
2.  **Staff Invitation**: Invite a staff member and verify they can see the corporate billing option in the app.
3.  **Real-Time Mandate**: Create an order via a `direct_debit` account and verify the Paystack API is hit immediately.
4.  **Invoice Generation**: Run the invoice job manually and verify a correctly formatted PDF is generated and emailed.
