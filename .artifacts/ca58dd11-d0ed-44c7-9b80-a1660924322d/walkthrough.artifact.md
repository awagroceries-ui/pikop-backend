# Walkthrough - Milestone 10: Testing Pass

I have implemented a comprehensive integration test suite for the Pikop backend to ensure the platform's reliability, security, and financial accuracy.

## Changes Made

### Test Infrastructure
- **Jest & Supertest**: Integrated standard Node.js testing tools into the project.
- **Database Isolation**: Created `tests/setup.js` to automatically clear and re-seed the database between tests, ensuring a clean state for every scenario.
- **Data Factories**: Developed `tests/factories.js` to quickly generate test users, fulfillers, and orders with standard or custom attributes.

### Test Suites
#### 1. Authentication (`auth.test.js`)
- Verified the complete signup flow and OTP record creation.
- Confirmed that valid credentials return both access and refresh tokens.
- **Security Check**: Verified that the `otpRateLimiter` correctly blocks brute-force attempts after 5 requests.

#### 2. Order Lifecycle & Race Conditions (`order.test.js`)
- **Atomic Order Matching**: Implemented a "Race Condition" test that sends simultaneous acceptance requests for the same order. Verified that the atomic `SELECT ... FOR UPDATE` logic correctly allows only one winner.
- **State Machine**: Verified the full flow from `MATCHED` -> `PICKED_UP` -> `DELIVERED`.
- **Code Verification**: Confirmed that only correct 4/6-digit codes can progress the order status.

#### 3. Wallet & Financial Accuracy (`order.test.js`)
- **Automated Payouts**: Verified that upon delivery completion, the fulfiller's wallet is credited with exactly 75% of the fare, and the platform commission wallet receives 25%.
- **Ledger Integrity**: Confirmed that financial records are correctly added to the immutable audit trail.

#### 4. Admin Protection (`admin.test.js`)
- Verified that protected admin routes correctly redirect unauthenticated users to the login page.

## How to Run Tests on Your VPS

> [!IMPORTANT]
> To execute these tests, follow these steps in your SSH terminal:
> 1. `cd backend`
> 2. `npm install` (to fetch Jest and Supertest)
> 3. `npm test`

The test runner is configured with `--runInBand` to ensure database transactions don't overlap across suites, and `--detectOpenHandles` to help identify any connection leaks.
