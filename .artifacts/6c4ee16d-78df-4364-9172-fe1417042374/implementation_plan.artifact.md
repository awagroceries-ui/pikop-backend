# Implementation Plan - Fix Quote Fetch 500 Error

This plan addresses the "service temporarily unavailable: error 500" occurring when fetching a delivery quote.

## Proposed Changes

### 1. Backend: Quote Generation Sanitization & Outer Exception Guard

#### [MODIFY] [orderController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/orderController.js)
- **Outer Try-Catch**: Wrap the entire `getQuote` handler in an outer `try-catch` block. If any unhandled exception occurs, log the full stack trace and return a clear JSON error response instead of crashing with a 500 status code.
- **Sanitize `undefined` Values**: PostgreSQL node-postgres driver throws a fatal error if any element in a `db.query()` parameter array is `undefined`. Sanitize all parameters (`pickup_state`, `pickup_landmark`, `delivery_landmark`, `recipient_phone`, `userId`, etc.) using nullish coalescing (`|| null` / `|| ''`) before passing them to database queries.
- **Surge Query Guard**: Guard the surge calculation queries (`demandRes`, `supplyRes`) so they only query by `pickup_state` when a valid state string is present.

---

## User Review Required

> [!IMPORTANT]
> **Deployment Requirement**
> Applying this fix on your server will require pulling the latest changes from Git and restarting PM2 (`pm2 restart pikop-v3`).

---

## Verification Plan

### Automated Tests
- Syntax check `orderController.js` using `node -c`.

### Manual Verification
1. Test quote generation in the app with incomplete fields (e.g. no landmarks or state specified).
2. Confirm the quote calculates successfully and returns a valid `quote_id` without 500 errors.
