# Milestone 2: Fulfiller 2.0 & Advanced Onboarding Task List

## Phase 1: Database & Schema
- [ ] Create `fulfiller_2_0` migration (`mobility_type`, `profile_photo_url`, `tier` for Fulfillers; new `vehicles` table)

## Phase 2: Backend Logic & Dispatch
- [ ] Update `fulfillerController.js`:
    - [ ] `updateProfile`: Support mobility type, vehicle details, and live photo
    - [ ] `submitApplication`: Validate class-specific requirements
- [ ] Update `dispatchService.js`:
    - [ ] Implement radius multiplier for `bicycle` Agents
- [ ] Update `orderController.js`:
    - [ ] Create `getPublicProfile` helper
    - [ ] Inject profile into Order/Offer responses

## Phase 3: Android: Advanced Onboarding
- [ ] Update `ApiService.kt` with new Fulfiller 2.0 endpoints
- [ ] Refactor `KycUploadScreen.kt`:
    - [ ] Implement mandatory Live Camera capture (gallery restricted)
    - [ ] Implement onboarding branch for Agents (Mobility Selection)
    - [ ] Implement onboarding branch for Riders/Drivers (Vehicle Details)

## Phase 4: User App: Fulfiller Profile
- [ ] Update `OrderCard` and `ActiveOrderScreen` to show the new Public Profile Card (Photo, Tier, Plate)

## Verification
- [ ] Verify live photo restriction (Camera only)
- [ ] Verify branching onboarding logic (Agent vs Rider)
- [ ] Verify Cyclist dispatch radius expansion
