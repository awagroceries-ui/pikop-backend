# Implementation Plan: Customized Welcome Emails per User Group

This plan implements tailored welcome emails for five distinct user groups: Customer, Agent/Fulfiller, Food Merchant, Groceries Merchant, and Shop Merchant.

## Infrastructure

- **Email Provider:** Transactional emails are currently sent via **Brevo (formerly Sendinblue)** using SMTP relay in `emailService.js`.
- **Trigger Timing:**
    - **Customer:** Sent immediately upon successful OTP verification during signup.
    - **Fulfiller/Merchant:** Sent only upon final account approval by an admin.

## Proposed Changes

### Backend Service: `emailService.js`
- **[MODIFY] `sendWelcomeEmail`**: Refactor to support the five distinct groups.
- **[NEW] Group-Specific Template Blocks**:
    - **Customer**: Focus on proximity ordering and escrow buyer protection.
    - **Agent/Fulfiller**: Personalized with their category (Rider, Driver, etc.) and payout info.
    - **Merchants (Food, Groceries, Shop)**: Include their specific category commission rates (10%, 5%, 10%) and COD acceptance status.

### Backend Controller: `authController.js`
- **[MODIFY] `verifyOtp`**: Update to only trigger `sendWelcomeEmail` for `CUSTOMER` role. Fulfillers and Merchants will have their emails deferred to the approval step.

### Backend Controller: `adminController.js`
- **[MODIFY] `updateKYCStatus`**: Extend to handle Merchants.
- **[NEW] Logic to trigger `sendWelcomeEmail`**: When an admin marks a Fulfiller or Merchant as `VERIFIED`, trigger the appropriate welcome email pulling real data (commission, category) from the DB.

## User Review Required

> [!IMPORTANT]
> **Dynamic Values in Templates**
> I will ensure that commission rates are fetched from `PlatformConfig` at send-time rather than hardcoded in the email text to prevent "rate drift" if settings change.

## Verification Plan

### Automated/Code Verification
- Verify `emailService.js` correctly maps user roles/categories to their respective templates.
- Verify `authController.js` gating logic (Customer vs others).

### Manual Verification
1. Register a new Customer. Verify immediate receipt of the "Superior Logistics" welcome email.
2. Approve a Fulfiller as Admin. Verify the email includes their specific mobility category (e.g. "Approved as a Rider").
3. Approve a Groceries Merchant. Verify the email explicitly states the 5% commission rate.
4. Approve a Food Merchant. Verify the email explicitly states the 10% commission rate.
