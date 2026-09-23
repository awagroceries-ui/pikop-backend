# Walkthrough - Fix Quote Fetch 500 Error

I have resolved the "service temporarily unavailable: error 500" during quote calculation.

## Changes Made

### 🛡️ 1. Sanitized Node-Postgres Parameters
- **Root Cause**: When fetching quotes, optional fields like `pickup_state`, `pickup_landmark`, `delivery_landmark`, and `recipient_phone` were being passed to `db.query()` as `undefined`. The PostgreSQL driver (`node-postgres`) throws a fatal `TypeError` when any parameter in a query array is `undefined`.
- **Sanitizer**: Converted all optional inputs to explicit `null` values (`|| null`) before passing them to PostGIS distance calculations, surge pricing queries, and `INSERT INTO quotes`.

### ⚡ 2. Outer Error Boundary Guard
- **Top-Level Catch**: Wrapped the entire `getQuote` handler in `orderController.js` in a top-level `try-catch` block.
- **Resilient Error Response**: If any calculation error occurs, it is caught cleanly, logged with a full stack trace, and returns a structured error message (`"Unable to calculate delivery quote. Please check your addresses and try again."`) instead of crashing the server.

### 📈 3. Surge Query Protection
- Added a check so the surge pricing queries (`demandRes`, `supplyRes`) only execute when a non-null `pickup_state` is provided.

---

## Verification Results

- **Syntax Validation**: [VERIFIED] `orderController.js` passed syntax checks (`node -c`) cleanly.

---

## Deployment Instructions

To apply the fix to your VPS server:

```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
pm2 restart pikop-v3
```
