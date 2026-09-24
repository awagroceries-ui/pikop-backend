# 🚀 VPS Production Notice

In the most recent deployment, the database migration (`1726930000000_add_kyc_provider_ref_to_users.js`) was pushed to GitHub but was **not executed** on the VPS before the PM2 restart.

Because the migration was skipped, the server is still throwing the `column "kyc_provider_ref" of relation "users" does not exist` PostgreSQL error during Sign Ups and Account Deletions.

---

### 🖥️ Action Required on Server

You must execute the database migration command explicitly on your VPS terminal (`root@srv1932412`).

Run these precise commands:

```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
npm run migrate:up
pm2 restart pikop-v3
pm2 logs pikop-v3 --lines 30
```

Once `npm run migrate:up` creates the `kyc_provider_ref` column in the `users` table, the SQL crashes will immediately stop.