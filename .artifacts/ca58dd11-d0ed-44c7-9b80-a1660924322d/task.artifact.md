# Milestone 3: Corporate/SME Infrastructure Task List

## Phase 1: Database & Schema
- [ ] Create `corporate_infrastructure` migration (`corporate_accounts`, `corporate_sub_accounts`, `orders` update)
- [ ] Extend `wallets` schema for corporate owners

## Phase 2: Backend Management Logic
- [ ] Create `corporateController.js` (Account init, staff management, mandate logic)
- [ ] Create `corporateRoutes.js`
- [ ] Register routes in `app.js`

## Phase 3: Payment & Dispatch Integration
- [ ] Update `walletService.js` to handle corporate debiting
- [ ] Update `orderController.js` to support corporate payment flow
- [ ] Implement basic direct-debit charge logic (Paystack integration stub)

## Phase 4: Admin Dashboard
- [ ] Update `adminController.js` with corporate account listing
- [ ] Create `corporate_accounts.ejs` view
- [ ] Update `layout.ejs` with "Corporate Accounts" link

## Phase 5: Invoicing System
- [ ] Create basic `monthlyInvoice` job script
- [ ] Implement PDF generation stub

## Verification
- [ ] Verify corporate sub-account link
- [ ] Verify real-time debit for corporate orders
- [ ] Verify Admin listing of corporate accounts
