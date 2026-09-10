# Task List - Logistics & Admin Refinements

- [ ] **Phase 1: Dispatch & Eligibility**
    - [ ] Update `getQuote` with size-to-class mapping
    - [ ] Implement Okada (Rider) per-zone dispatch filtering
    - [ ] Update Android Match Card with `FulfillerPublicProfile`
- [ ] **Phase 2: Landmarks**
    - [ ] Backend: Implement landmark proximity matching and content check
    - [ ] API: Implement `GET /addresses/landmark-suggestions`
    - [ ] Android: Add required landmark field to Order Creation
- [ ] **Phase 3: Failed Delivery & Consent**
    - [ ] Backend: Implement 10-minute timeout and 50/50 payout split
    - [ ] Web: Create Public Delivery Consent page
    - [ ] Android: Implement "Request Consent" button and photo-metadata capture
- [ ] **Phase 4: Weather & Traffic**
    - [ ] Backend: Implement Google Weather API 15-min polling job
    - [ ] Backend: Implement Traffic Corridor rush-hour multipliers
- [ ] **Phase 5: Admin Dashboard**
    - [ ] Dashboard: Landmark Suggestions management screen
    - [ ] Dashboard: Traffic Corridor and Zone Okada-toggle screens
