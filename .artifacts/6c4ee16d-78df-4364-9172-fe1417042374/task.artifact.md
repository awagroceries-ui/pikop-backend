# Task: Advanced Incident & Dispute Engine

- [x] **Infrastructure & Schema**
    - [x] Create migration `1726490000000_incident_management.js`
- [x] **Backend Logic - Incident Management**
    - [x] Update `orderController.js` (`fileIncident`, `reportProblem`)
    - [x] Implement severity-based 3-way bridging logic
- [x] **Financial Resilience (Waivers)**
    - [x] Update `walletService.js` to support automated waivers
- [x] **Admin Resolution Dashboard**
    - [x] Update `adminController.js` with `getDisputeResolutionCenter`
    - [x] Create/Update EJS views for dispute resolution
- [x] **Android UI - Structured Reporting**
    - [x] Update `ActiveOrderScreen.kt` (Fulfiller incidents)
    - [x] Update `TrackOrderScreen.kt` (Customer disputes)
- [ ] **Verification**
    - [ ] Test 3-way bridge creation
    - [ ] Verify waiver reversal in wallet
    - [x] Build and Deploy
