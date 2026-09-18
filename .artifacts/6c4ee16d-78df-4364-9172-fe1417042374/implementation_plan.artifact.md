# Implementation Plan - Group-Specific FAQ Integration

This plan restructures the FAQ/Knowledge Base system to be user-group specific and category-organized, serving the content from the backend based on the provided `Pikop_FAQs_Content.md`.

## Proposed Changes

### 1. Backend Infrastructure (Node.js)

#### [MODIFY] [Knowledge Base Migration](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1723860000000_v3_knowledge_base.js)
- Update `target_audience` constraint to include `MERCHANT`.
- Add `CORPORATE` if needed (not in source content, but good for future-proofing).

#### [NEW] [Seed FAQs Migration](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1726570000000_seed_structured_faqs.js)
- Clear existing `knowledge_base` entries.
- Parse and insert all content from `Pikop_FAQs_Content.md` mapped to `CUSTOMER`, `FULFILLER`, and `MERCHANT` groups.

#### [MODIFY] [supportController.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/controllers/supportController.js)
- Update `getKnowledgeBase` to use the authenticated user's actual role (`req.user.role`) instead of defaulting everything to CUSTOMER/FULFILLER.
- Support a `group` query parameter for multi-role users to switch between FAQ sets.

### 2. Android App Integration (Compose)

#### [MODIFY] [SupportHubScreen.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/feature/auth/SupportHubScreen.kt)
- **Role Switcher**: If a user has multiple roles (e.g. CUSTOMER and MERCHANT), show a tab-style switcher at the top.
- **Search Bar**: Add a persistent search field at the top to filter categories or questions.
- **Collapsible Sections**: Instead of navigating to a new screen for each category, implement an accordion-style view where categories can be expanded to show questions directly.
- **Direct Navigation**: Clicking a question should still navigate to `FaqDetailScreen` for the full answer text.

#### [MODIFY] [ApiService.kt](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/app/src/main/java/com/ng/pikop/core/network/ApiService.kt)
- Update `getKnowledgeBase` to optionally accept a `user_group` parameter.

## User Review Required

> [!IMPORTANT]
> **Data Migration**
> I will replace all current FAQ content with the provided structured markdown content. Any manual additions made to the `knowledge_base` table in production should be backed up or re-added after this migration.

## Verification Plan

### Manual Verification
1.  **Role Awareness**: Log in as a Fulfiller. Verify only "For Fulfillers / Agents" content is visible.
2.  **Category Organization**: Verify questions are grouped under headers like "Getting Started", "Missions", etc.
3.  **Search**: Type "SOS" in the Fulfiller FAQ search. Verify the relevant mission question appears.
4.  **Multi-Role**: Log in as a user with both `CUSTOMER` and `MERCHANT` roles. Verify a toggle appears to switch between "Customer Help" and "Seller Help".
5.  **Text Visibility**: Confirm long FAQ answers are fully visible and not cut off (re-verifying previous fix).
