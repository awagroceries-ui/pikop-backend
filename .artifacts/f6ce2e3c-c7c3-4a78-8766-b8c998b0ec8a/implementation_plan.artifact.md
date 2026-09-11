# Implementation Plan - SMS Resilient Delivery Fallback

This plan implements a resilient fallback mechanism for SMS delivery to bypass the "Country Inactive" error currently being returned by Termii for the DND route.

## User Review Required

> [!IMPORTANT]
> **Automatic Route Testing:**
> - I am implementing an "Automatic Fallback." If the server tries to send an OTP via the **DND channel** and Termii rejects it (e.g., "Country Inactive"), the server will **immediately retry** via the **Generic channel**.
> - This should resolve the delivery issue if your Termii account has the Generic route active but is still waiting for DND activation.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [smsService.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/services/smsService.js)
- **Channel Configuration:** Allow the primary channel to be set via `TERMII_CHANNEL` (defaulting to `dnd`).
- **Resilient `sendOtp`**:
    - Add a retry loop.
    - If the first attempt returns an error containing "Country Inactive" or "Route," immediately attempt a second send using the `generic` channel.
- **Improved Logging:** Clearly log when a fallback is triggered so we can see it in the VPS logs.
- **Sender ID Logic:** Ensure "N-Alert" is consistently used as the fallback Sender ID for all channels.

---

## Verification Plan

### Manual Verification (VPS Logs)
1. **Trigger Signup/Resend:** Attempt to get an OTP in the app.
2. **Audit Logs:** Run `pm2 logs pikop-v3`.
3. **Expected Result:**
    - You may see one `[Termii] OTP fail` for the DND route.
    - Immediately followed by `[Termii] Falling back to generic channel...`.
    - Then a successful delivery log if the generic route is active.
4. **Final Confirmation:** Check your phone for the SMS from "N-Alert" or "Pikop".
