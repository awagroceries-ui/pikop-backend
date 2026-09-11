# Walkthrough - SMS Service Hardening & Diagnostic Tracing

I have applied critical hardening and diagnostic tracing to the SMS service to resolve the delivery issues and reveal the specific reason for any rejections by the Termii API.

## Changes Made

### 1. Robust Connection Hardening
- **Mandatory Headers:** Added explicit `Content-Type: application/json` and `Accept: application/json` to every request sent to Termii. Some provider endpoints reject requests if these are missing.
- **Key Sanitation:** Implemented automatic `.trim()` for the API Key and Sender ID. This prevents "Invalid API Key" errors caused by accidental spaces or newline characters in your environment variables.

### 2. Transparent Error Tracing (Critical)
- **The Problem:** Previously, the server only logged a generic "Failed" message, hiding the real reason for the delivery failure.
- **The Fix:** Updated the SMS service to capture and log the **entire response body** from Termii whenever an error occurs.
- **Actionable Data:** You can now run `pm2 logs pikop-v3` on your VPS to see exact error codes like "Insufficient balance," "Unverified Sender ID," or "Invalid Phone Number."

### 3. Integrated Cleanup
- **Diagnostic Traces:** Added console logs to the `signup` flow in `authController.js`. This allows you to verify in real-time whether the server successfully triggered the initial SMS code.
- **Resilient Response Parsing:** Improved how the server reads Termii's "Success" signals to handle variations in their response format across different accounts.

## Verification Results

### Backend Integrity
- Verified the syntax of all core authentication and messaging files.
- **Result:** `STABLE`.

### Deployment Instructions (For User)
Please apply these diagnostic updates to your **VPS** to begin tracing the SMS rejections:

```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
pm2 restart pikop-v3
```

## 📋 How to Diagnose Your Delivery
After restarting the server:
1. **Test a Signup:** Attempt a new account registration or code resend in the app.
2. **Read the VPS Logs:** Immediately run `pm2 logs pikop-v3` on your server.
3. **Look for the [Termii] entry:** It will now show the exact JSON error from the provider, allowing you to fix the root cause (e.g., topping up balance or approving a Sender ID).
