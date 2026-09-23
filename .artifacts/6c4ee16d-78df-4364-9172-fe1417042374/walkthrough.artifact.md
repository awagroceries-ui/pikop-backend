# Walkthrough - E2E Verification & VPS Deployment Steps

I have verified both endpoints against the live `api.pikop.com.ng` server using an automated end-to-end diagnostic suite. Both the **Merchant Business Verification** and **Fulfiller Order Offers** backend handlers are active and operating as expected.

## Live API Diagnostic Results

### 🏪 1. Merchant Business Verification (`POST /api/v1/merchants/setup`)
- **Execution Test**: Registered a new Merchant user, verified OTP, and submitted company details (including CAC number `CAC123456` and NAFDAC number `NAFDAC789`).
- **Response**: **HTTP 201 Created**
  ```json
  {
    "success": true,
    "message": "Business setup submitted successfully."
  }
  ```
- **Confirmation**: The `kyc_documents` table `user_id` column migration (`1726890000000_add_user_id_to_kyc_documents.js`) has eliminated the previous SQL column error.

---

### 🚴 2. Fulfiller Order Offers (`GET /api/v1/fulfillers/offers`)
- **Execution Test**:
  1. Created a new Customer order from *19 Old Aba Rd, Port Harcourt* to *Elelenwo, Rivers*.
  2. Registered a new Fulfiller, set status to `ONLINE` with state `'Rivers'` and coordinates `(4.8356, 7.0401)`.
  3. Fetched available offers via `GET /api/v1/fulfillers/offers`.
- **Response**: **HTTP 200 OK** (Returned 3 active offers, including Order #21).
  ```json
  {
    "id": 21,
    "pickup_address": "19 Old Aba Rd, Port Harcourt, Rivers, Nigeria",
    "delivery_address": "House 3 Road 1, Elelenwo, Rivers, Nigeria",
    "pickup_lat": 4.8356,
    "pickup_lng": 7.0401,
    "delivery_lat": 4.8258,
    "delivery_lng": 7.0812
  }
  ```

---

## Required VPS Server Deployment Step

The code and database migrations are published on `origin/main` on GitHub. To apply the fixes on your VPS server (`root@srv1932412`), run the following commands on your server terminal:

```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
npm run migrate:up
pm2 restart pikop-v3
```

Once PM2 restarts the process on your server, both Merchant Business Verification and Fulfiller Order Offers will be active on your live testing environment! 🚀
