# 🚀 Walkthrough: Knowledge Base Fee Articles & EJS Views Alignment

Updated all Knowledge Base articles in PostgreSQL and Admin EJS views to accurately state the active platform fee rates:
- **COD / Escrow Platform Fee**: **5%** (formerly 10%)
- **Dispatch Commission**: **20%** (formerly 25%, meaning Fulfillers keep **80%**)
- **Merchant Marketplace Commission**: **5% across all categories** (Food: 5%, Groceries: 5%, Shop: 5%)

---

## 🛠️ Summary of Implementation

### 1. Migration & Execution Script (`backend_v3`)
- Created [1726960000000_update_kb_fee_articles.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/migrations/1726960000000_update_kb_fee_articles.js):
  - Database migration updating the `content` column of affected `knowledge_base` rows with exact 5% COD fee, 20% dispatch commission (80% fulfiller share), and 5% merchant commission text.
- Created [update_kb_fees.js](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/update_kb_fees.js):
  - Standalone execution script to run fee article updates on local and VPS database instances.

### 2. Admin EJS Views (`backend_v3/src/views/`)
- Updated [guest_checkout.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/guest_checkout.ejs): `Platform Fee (5%)`.
- Updated [financial_overview.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/financial_overview.ejs): `Commission (20%)` and `Escrow Fees (5%)`.
- Updated [fleet_partners.ejs](file:///C:/Users/MOSES/AndroidStudioProjects/Pikop/backend_v3/src/views/fleet_partners.ejs): `DEFAULT (20%)`.

### 3. RAG-AI Support Integration
- Because `askPikopAgent` in `supportController.js` pulls articles directly from the `knowledge_base` table, updating these rows ensures the Gemini AI Support Agent automatically answers user questions with the exact 5% COD fee, 20% dispatch commission (80% agent share), and 5% merchant commission.

---

## 🧪 VPS Deployment Instructions

Run the command below on your VPS terminal (`root@srv1932412`) to run the migration, update the Knowledge Base articles, and restart PM2:

```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
npm run migrate:up
node update_kb_fees.js
pm2 restart pikop-v3
pm2 logs pikop-v3 --lines 30
```
