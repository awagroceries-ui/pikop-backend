# Task: Fix Missing FAQ Answers

- [ ] **Backend Hardening**
    - [x] Create `getArticleById` endpoint logic
    - [x] Update `supportRoutes.js`
    - [x] Generate `1726730000000_fix_faq_content.js` migration from latest MD
- [ ] **Android API & Models**
    - [x] Update `ApiService.kt` with `getArticleById`
- [ ] **Android UI Fixes**
    - [x] Update `FaqDetailScreen.kt` to fetch by ID (Fixes "Missing Answer" for multi-role users)
- [ ] **Verification**
    - [ ] Run migration on server
    - [ ] Verify full content display for all user groups
    - [ ] Build and Deploy
