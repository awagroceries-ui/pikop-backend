# Implementation Plan - Fix Missing FAQ Answers

This plan addresses the data issue where FAQ answers were being lost or not displayed correctly, and re-imports the full structured content from the updated `Pikop_FAQs_Content(1).md`.

## Proposed Changes

### 1. Backend Hardening (Node.js)

#### [MODIFY] [supportRoutes.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/routes/supportRoutes.js)
- Add a new endpoint `GET /kb/:articleId` to fetch a specific FAQ article by its ID.

#### [MODIFY] [supportController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/supportController.js)
- Implement `getArticleById`: Fetches a single article directly from the `knowledge_base` table.

#### [NEW] [Refined FAQ Seeding](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1726730000000_fix_faq_content.js)
- Clear existing FAQ entries.
- Re-import 57 high-depth FAQ entries parsed directly from the latest source file using a robust automation script to ensure no data loss.
- Use PostgreSQL dollar-quoting (`$$`) to preserve all formatting, quotes, and symbols.

### 2. Android App Integration (Compose)

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Register the new `getArticleById` method.

#### [MODIFY] [FaqDetailScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/FaqDetailScreen.kt)
- **Bug Fix**: Switch from finding the article in the general list (which was group-filtered and caused "Missing Answer" bugs for multi-role users) to fetching it directly by ID using the new API endpoint. This ensures the answer is *always* found regardless of the user's current role or group.

## User Review Required

> [!IMPORTANT]
> **Data Restoration**
> This fix involves a full reload of the `knowledge_base` table. The new content is significantly more detailed (57 entries vs the previous ~20).

## Verification Plan

### Manual Verification
1.  **Direct Navigation**: Log in as a MERCHANT. Search for a CUSTOMER FAQ. Click it and verify the full, long answer is displayed in `FaqDetailScreen`.
2.  **Completeness Audit**: Scroll through every category in the Help Center. Confirm every single question has a corresponding answer.
3.  **Search & Filter**: Verify that searching for "COD" or "Refund" returns the correct list and those articles load their answers perfectly.
4.  **Layout & Visibility**: Confirm that the long, multi-paragraph answers are scrollable and the text is high-contrast against the background.
