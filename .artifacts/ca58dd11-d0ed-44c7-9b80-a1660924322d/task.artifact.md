# Milestone 1: Platform Hardening [IN PROGRESS]

Establishing production-grade security, dispatch logic, and operational baseline.

## Phase 1: Database & Crypto
- [ ] Create `hardened_baseline` migration (`pickup_code_hash`, `capture_metadata`)
- [ ] Implement `crypto.randomInt` and `bcrypt` hashing for verification codes

## Phase 2: Refined Dispatch & Eligibility
- [ ] Update `orderController.js` with delayed `delivery_code` generation
- [ ] Update `dispatchService.js` with class-based notification filtering
- [ ] Add `resendOtp` with rate limiting in `authController.js`

## Phase 3: Android Security & POD
- [ ] Force Camera capture in `ActiveOrderScreen.kt`
- [ ] Attach GPS/Timestamp metadata to delivery verification
- [ ] Add "Resend Code" with cooldown in `EmailOtpScreen.kt`

## Verification
- [ ] Test cross-class offer filtering (Driver vs Small Order)
- [ ] Verify arrival-triggered delivery code generation
- [ ] Confirm GPS-locked delivery photo metadata
