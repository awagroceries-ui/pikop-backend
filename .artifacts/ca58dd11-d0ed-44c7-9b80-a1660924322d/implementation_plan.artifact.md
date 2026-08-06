# Implementation Plan - Milestone 10: Testing Pass

Implement comprehensive integration tests for the Pikop backend to ensure the reliability of the order state machine, financial transactions, and security layers.

## User Review Required

> [!IMPORTANT]
> - **Test Framework**: I will use **Jest** and **Supertest**. This is the standard for Node.js/Express and provides a fast, reliable test runner.
> - **Database**: Tests will run against the database configured in `.env`. I will implement a `test-setup.js` to clear tables before each test suite to ensure isolation.
> - **Mocks**: I will mock external dependencies like the Gemini API and Paystack Transfers API to ensure tests are fast and don't incur costs or depend on external connectivity.

## Proposed Changes

### Configuration

#### [MODIFY] `backend/package.json`
- Add `jest` and `supertest` as dev dependencies.
- Add a `test` script.

---

### Test Infrastructure

#### [NEW] `backend/tests/setup.js`
- Utility to clear database tables and handle global setup/teardown.

#### [NEW] `backend/tests/factories.js`
- Helpers to quickly create test users, fulfillers, and orders.

---

### Test Suites

#### [NEW] `backend/tests/auth.test.js`
- Test signup logic and OTP generation.
- Test login and JWT token issuance.
- Test `otpRateLimiter` effectiveness.

#### [NEW] `backend/tests/order.test.js`
- **State Machine**: Verify transitions (`SEARCHING` -> `MATCHED` -> `PICKED_UP` -> `DELIVERED`).
- **Race Condition**: Simulate multiple fulfillers accepting the same order simultaneously and verify only one succeeds (Atomic `SELECT ... FOR UPDATE` test).
- **Pricing**: Verify fare calculation based on size tiers.

#### [NEW] `backend/tests/wallet.test.js`
- **Commission Split**: Verify the 75/25 split arithmetic upon delivery.
- **Withdrawal**: Test balance validation and withdrawal ledger entry creation.

#### [NEW] `backend/tests/admin.test.js`
- **RBAC**: Verify that different admin roles can only access their permitted resources.

---

## Verification Plan

### Automated Tests
- Run `npm test` and verify that all test suites pass.
- Monitor console output for the race condition test specifically.

### Manual Verification
- Verify that running tests does not affect the persistence of the development/production database data (by using a separate test DB if configured).
