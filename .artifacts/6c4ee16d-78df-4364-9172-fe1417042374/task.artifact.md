# Task: Fix Quote Fetch 500 Error

- [ ] **Backend Quote Handler Fix**
    - [ ] Wrap `getQuote` in top-level `try-catch` block
    - [ ] Sanitize all optional query parameters (`pickup_state`, `pickup_landmark`, `delivery_landmark`, `recipient_phone`, `userId`) from `undefined` to `null`
    - [ ] Guard surge calculation queries against `undefined` state
- [ ] **Verification & Deployment**
    - [ ] Verify syntax using `node -c`
    - [ ] Commit and push changes to GitHub
