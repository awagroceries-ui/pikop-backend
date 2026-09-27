# 📋 Implementation Plan: Update Knowledge Base Articles & EJS Views for Active Platform Fees

Update all Knowledge Base articles (`knowledge_base` database table) and Admin EJS views to accurately reflect the active platform fee rates:
- **COD / Escrow Platform Fee**: **5%** (formerly 10%)
- **Dispatch Commission**: **20%** (formerly 25%, meaning Fulfillers keep **80%**)
- **Merchant Marketplace Commission**: **5% across all categories** (Food: 5%, Groceries: 5%, Shop: 5%)

---

## 🔍 Research & RAG-AI Integration Analysis

1. **AI Support Agent Integration (`supportController.js`)**:
   - `askPikopAgent` performs RAG-lite retrieval on the `knowledge_base` table:
     `SELECT title, content FROM knowledge_base WHERE is_active = true AND ...`
   - Updating articles in the `knowledge_base` table ensures the Gemini AI Support Agent automatically reads and answers user questions with the correct 5% COD Fee, 20% Dispatch Commission (80% Fulfiller share), and 5% Merchant Marketplace Commission.

2. **Affected Knowledge Base Articles**:
   - `Is there a fee for using COD?`: Update fee to 5%.
   - `Why does the checkout total sometimes include a fee, and sometimes not?`: Update fee to 5%.
   - `How much do I actually earn per delivery?`: Update earnings share to 80% (20% commission).
   - `What commission does Pikop actually take, and how is it calculated?`: Update category commission to 5% across Food, Groceries, and Shop.
   - `Why is the Groceries commission lower than Food and Shop?`: Update to reflect unified 5% merchant commission across all marketplace categories.
   - `If I accept COD, does the fee cost me anything?`: Update COD fee to 5%.

3. **Affected EJS Views**:
   - `guest_checkout.ejs`: `Platform Fee (5%)`.
   - `financial_overview.ejs`: `Commission (20%)` and `Escrow Fees (5%)`.
   - `fleet_partners.ejs`: `DEFAULT (20%)`.

---

## 🛠️ Proposed Changes

### Component 1: Migration & Maintenance Script (`backend_v3`)

#### [NEW] [1726960000000_update_kb_fee_articles.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1726960000000_update_kb_fee_articles.js)
- Database migration updating the `content` column of affected `knowledge_base` rows with exact 5% COD fee, 20% dispatch commission (80% fulfiller share), and 5% merchant commission text.

#### [NEW] [update_kb_fees.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/update_kb_fees.js)
- Standalone Node.js execution script to run `update_kb_fees.js` directly on local and VPS database instances.

---

### Component 2: EJS Views Updates (`backend_v3/src/views/`)

#### [MODIFY] [guest_checkout.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/guest_checkout.ejs)
- Update fee text to `Platform Fee (5%)`.

#### [MODIFY] [financial_overview.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/financial_overview.ejs)
- Update commission text to `Commission (20%)` and `Escrow Fees (5%)`.

#### [MODIFY] [fleet_partners.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/fleet_partners.ejs)
- Update default commission text to `DEFAULT (20%)`.

---

## 🧪 Verification Plan

### Execution & Verification Steps
1. Execute `node update_kb_fees.js` locally.
2. Verify `knowledge_base` table contents:
   `SELECT title, content FROM knowledge_base WHERE title LIKE '%fee%' OR title LIKE '%commission%';`
3. Stage, commit, and push changes to GitHub `main`.
4. Deploy to production VPS server (`root@srv1932412`) and run `npm run migrate:up` & `node update_kb_fees.js`.
5. Restart PM2 process (`pm2 restart pikop-v3`).
