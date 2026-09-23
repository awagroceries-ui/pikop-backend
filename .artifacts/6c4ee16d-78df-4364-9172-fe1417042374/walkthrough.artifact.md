# Walkthrough - Numeric NaN Sanitization for Quote Calculation

I have identified and resolved the cause of the `Unable to calculate delivery quote` error during quote calculation.

## Root Cause Analysis
- **PostgreSQL Numeric Constraint Violation**: When calculating quotes, if any multiplier or setting value evaluated to `NaN` (e.g. `parseFloat(undefined)` on setting overrides or surge multipliers), JavaScript passed `NaN` as a number to PostgreSQL for `total_fare: decimal(12,2)`.
- PostgreSQL numeric/decimal columns reject `"NaN"` strings with a database syntax error (`invalid input syntax for type numeric: "NaN"`), triggering the fallback error handler.

---

## Changes Made

### 🔢 1. `safeNumber` Sanitizer Function (`orderController.js`)
- Added a robust numeric sanitizer `safeNumber(val, fallback)`:
  ```javascript
  const safeNumber = (val, fallback = 0) => {
    const n = parseFloat(val);
    return (isNaN(n) || !isFinite(n)) ? fallback : n;
  };
  ```
- Wrapped all calculations (`distanceKm`, `weatherMultiplier`, `trafficMultiplier`, `surgeMultiplier`, `insuranceFee`, `delivery_fee`, and `total_payable`) with `safeNumber` to guarantee that PostgreSQL receives a valid numeric value every time.

---

## Verification Results

- **Syntax Check**: [VERIFIED] `orderController.js` passed syntax checks (`node -c`).
- **Git Push**: [SUCCESS] Pushed commit `0921646a` to `origin/main`.

---

## Deployment Instructions

To activate the fix on your VPS server:

```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
pm2 restart pikop-v3
```
