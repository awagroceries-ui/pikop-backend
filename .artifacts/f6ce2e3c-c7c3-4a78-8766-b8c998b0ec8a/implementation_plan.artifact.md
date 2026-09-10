# Implementation Plan - Logistics Hardening & Admin Refinements

This plan outlines the systematic implementation of missing logistics features, including crowdsourced landmarks, Okada zoning, weather/traffic surcharges, and comprehensive failed-delivery handling.

## Phased Approach

### Phase 1: Dispatch & Eligibility Hardening
Enforce strict rules on which fulfillers see which orders based on size and regional restrictions.

- **Class-Based Eligibility:** Update `getQuote` and dispatch logic to map `small`/`medium`/`large` sizes to `agent`/`rider`/`driver`.
- **Okada (Rider) Zoning:** Intersect size eligibility with the pickup zone's `allowed_fulfiller_classes` to respect Lagos LGA restrictions.
- **Android Match UI:** Update the match card to show the full `FulfillerPublicProfile` (Photo, Name, Tier Badge, Rating).

### Phase 2: Landmarks & Geocoding accuracy
Improve "cold-arrival" accuracy by requiring and crowdsourcing landmarks.

- **Automated Suggestions:** Implement backend logic to auto-approve landmarks and increment submission counts based on proximity (~200m).
- **Landmark Content Check:** Add a basic profanity/abuse filter for new landmark submissions.
- **Android Landmark Entry:** Make `landmark_description` a required field in order creation and saved addresses with real-time autocomplete suggestions.

### Phase 3: Failed Delivery & Consent Flow
Formalize the "Plan B" for when a recipient isn't present or reachable.

- **Compensation Rule:** Implement the 10-minute timeout at drop-off. If unreachable, Fulfiller can mark as "Failed".
- **Settlement:** The mission fare is settled using the **standard 75/25 split** (Fulfiller gets 75%, Platform takes 25% commission). This ensures the Fulfiller is compensated for the full trip distance even if the recipient is unavailable.
- **Delivery Consent:** Add the "Request Consent" flow where a recipient approves a "leave at door" action via a public web link, replacing the delivery code with a metadata-rich photo.
- **Return Flow:** Prompt the User to pay for a return mission (at 50% fare) or abandon the item.

### Phase 4: Dynamic Adjustments (Weather & Traffic)
Implement automated and manual surcharges for environmental conditions.

- **Traffic Corridors:** Implement multiplier logic for known congestion axes (e.g., Third Mainland Bridge) during rush hours.
- **Weather Surcharges:** Set up a 15-minute polling job using Google Weather API to apply multipliers to flood-prone zones during alerts.
- **Ops Overrides:** Allow admin to manually pause dispatch in specific zones or override weather multipliers.

### Phase 5: Admin Dashboard Refinements
Build the operational interfaces to manage all the above.

- **Landmark Suggestions Screen:** Searchable list of crowdsourced landmarks for spot-checks.
- **Traffic/Zone Controls:** UI to edit traffic corridors and per-zone Fulfiller class toggles.
- **Financial Audit:** Ensure traffic and weather adjustments are visible as separate line items in the admin ledger.

## Verification Plan

### Automated Tests
- Build Android app: `./gradlew assembleDebug`.
- Syntax check backend logic.

### Manual Verification
1. **Dispatch:** Verify a Driver never receives a "Small" (Agent/Rider) offer.
2. **Zoning:** Seed a zone to disallow Riders; verify Riders in that zone see zero offers.
3. **Landmarks:** Enter a new landmark in the app; verify it becomes an autocomplete option for a different user in the same area.
4. **Compensation:** Simulate a 10-minute timeout and verify the 50% wallet credit to the Fulfiller.
