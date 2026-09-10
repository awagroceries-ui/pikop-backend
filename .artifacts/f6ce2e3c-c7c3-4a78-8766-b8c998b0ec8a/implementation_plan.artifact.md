# Implementation Plan - Fix SMS OTP Delivery & Integrate Keys

This plan addresses the issue where SMS verification codes are not being delivered to users, ensuring the signup flow remains functional, and integrates the provided Termii keys.

## User Review Required

> [!IMPORTANT]
> **Key Integration:** I will update the server to use the provided Termii Live API Key and Signing Secret.
>
> **Reliability Upgrade:** All OTPs will now be sent via the **DND channel** to ensure delivery even to restricted numbers.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [phone.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/utils/phone.js)
- Add `formatForTermii(phone)`: Strips the `+` from normalized numbers (e.g., `+234...` -> `234...`) as required by the Termii API.

#### [MODIFY] [smsService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/smsService.js)
- **Keys:** Ensure `TERMII_API_KEY` defaults to `tlv_vNooxh-VZNQ4yFmywjNwA5DxC1KdgDkLZYRXOHqtkys`.
- **Formatting:** Apply `formatForTermii` to all recipient numbers.
- **Channel:** Switch `channel` from `"generic"` to `"dnd"` for both generic SMS and OTPs.
- **Logging:** Improve error logs to capture the specific reason for delivery failures.

#### [MODIFY] [webhookController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/webhookController.js)
- Update `handleTermiiWebhook` to use the provided Signing Secret: `tsk_aMngGOk22bKBmOATkpceSlKtoG` as the authorized token.

#### [MODIFY] [.env.example](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/.env.example)
- Add `TERMII_WEBHOOK_SECRET` and update placeholders.

---

## Verification Plan

### Automated Tests
- Syntax check backend: `node -c ...`.

### Manual Verification
1.  **Direct API Test:** Trigger a signup and check the backend logs (`pm2 logs pikop-v3`).
2.  **DND Delivery:** Verify SMS arrival on a number with DND active.
3.  **Webhook Audit:** Verify that Termii delivery reports are authorized correctly using the new secret.
