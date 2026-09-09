# Implementation Plan - Fix Admin Dashboard Visibility & KYC File Rendering

This plan resolves the issues where Prembly reports were not visible to admins and KYC images/documents were not rendering in the dashboard.

## Problem Description
1.  **Broken Image Rendering:** Fulfiller photos and documents use relative paths (e.g., `/uploads/...`). These failed to load because the server lacked a static route for the `uploads/` directory, and the dashboard lacked an absolute `BASE_URL` context.
2.  **Invisible Prembly Reports:** Successful verification results from Prembly were stored in the database but were not correctly parsed or displayed in the admin review screen.

## Proposed Changes

### Backend (`backend_v3`)

#### [MODIFY] [app.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/app.js)
- **Static Route:** Add `app.use('/uploads', express.static(path.join(process.cwd(), 'uploads')))` to expose the submitted documents to the web.

#### [MODIFY] [adminController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/adminController.js)
- **Report Parsing:** Ensure `kyc_details` is parsed into a JSON object before being passed to the view.
- **Context Injection:** Pass the absolute `BASE_URL` to all KYC-related views.

#### [MODIFY] [kyc_review.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/kyc_review.ejs)
- **Absolute Paths:** Update all `<img>` and `<a>` tags to use the injected `baseUrl`.
- **Identity Display:** Refactor the report section to extract and display key identity fields from Prembly (e.g., Verified Name, DOB, Document Number) in a structured table.

---

## Verification Plan

### Automated Tests
- Syntax check backend: `node -c ...`.
- Verify static route: Access `https://api.pikop.com.ng/uploads/test.jpg` (after restart).

### Manual Verification
1.  **Dashboard Rendering:** Open an agent's application in the Admin Dashboard. All photos and documents should now be visible as images or clickable links.
2.  **Report Visibility:** Verify that the "Automated Verification Report" section now shows the fulfiller's verified name and document status from Prembly.
3.  **Audit Trail:** Confirm the "Raw JSON" section can be expanded to view the full Prembly payload.
