# Implementation Plan - Milestone 5: Full Account Hub & Platform Polish

Implement the complete Account/Settings menu for both app roles and unlock the full suite of Paystack payment channels.

## User Review Required

> [!IMPORTANT]
> - **Session Management**: I will introduce a `user_sessions` table to track every device logged into an account. This allows you to remotely log out specific devices for enhanced security.
> - **Payment Flexibility**: I am removing restrictions on Paystack channels. Users will now be able to pay via **USSD, Bank Transfer, QR Code**, and more, in addition to cards.
> - **Data Privacy**: The "Delete Account" feature will follow a "Request & Queue" pattern to comply with NDPA regulations, rather than instant deletion.

## Proposed Changes

### 1. Database & Schema (Backend)

#### [NEW] `backend/migrations/1723040000000_settings_and_sessions.js`
- **`user_sessions`**: `id`, `user_id`, `refresh_token`, `device_name`, `ip_address`, `last_active`.
- **`saved_recipients`**: `id`, `user_id`, `name`, `phone`, `label`.
- **`users`**: Add `notification_prefs` (jsonb), `language` (varchar, default 'en').
- **`fulfillers`**: Add `preferred_hours` (jsonb) for availability scheduling.

---

### 2. Full Settings Logic (Backend)

#### [NEW] `backend/src/controllers/settingsController.js`
- **`updateProfile`**: Handles name, photo, and sensitive changes (email/phone).
- **`manageRecipients`**: CRUD for the new favorites list.
- **`manageSessions`**: List and revoke active refresh tokens.
- **`requestAccountDeletion`**: Queues an NDPA-compliant deletion request.

#### [MODIFY] `authController.js`
- Update `login` and `signup` to register sessions in the `user_sessions` table.

---

### 3. Payment Polish (Prompt 9)

#### [MODIFY] `CheckoutHelper.kt` (Android)
- Omit restricted channels to surface all options enabled on your merchant account.

#### [MODIFY] `paymentController.js` (Backend)
- Update the `charge.success` webhook to capture the specific `channel` (ussd, qr, bank) and write it to `Order.payment_method`.

---

### 4. UI Refinement (Android)

#### [NEW] Settings Screens
- **`ProfileEditScreen.kt`**: Tabbed interface for personal and role-specific data.
- **`NotificationSettingsScreen.kt`**: Granular toggles for Push, SMS, and Email.
- **`RecipientManagementScreen.kt`**: Quick-select favorites for the order flow.

#### [MODIFY] `MainAppScaffold.kt`
- Expand the **Menu** tab with a high-end settings list UI (grouped by Security, Preferences, and Legal).

---

## Verification Plan

### Manual Verification
1.  **Multi-Channel Payment**: Perform a test purchase using the **USSD** channel; verify the order is created and the payment method is marked as "ussd" in the DB.
2.  **Session Security**: Log in on two different devices. From device A, go to "Manage Devices" and log out device B. Verify device B is instantly returned to the login screen on its next request.
3.  **Recipient Shortcut**: Add a recipient to your favorites, then start a new order and verify you can select them in one tap.
4.  **Profile Change**: Update your email in "Edit Profile" and verify the app forces a re-verification before the change is finalized.
