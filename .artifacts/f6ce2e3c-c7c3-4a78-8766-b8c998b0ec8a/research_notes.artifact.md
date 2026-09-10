# Research Notes - Wallet Top-Up Discrepancy

I have performed a deep dive into the wallet and payment logic. Here are the evidenced findings regarding the "missing money" issue.

## 1. The Fulfiller/User Wallet Split (Root Cause for Agents)
The system currently fragment's a single person's money into two separate wallets based on their active role:
- **`owner_type = 'USER'`**: Used for **Top-ups**, **Secure Pay Escrow Holds**, and **Referral Bonuses**.
- **`owner_type = 'FULFILLER'`**: Used for **Mission Settlement (75% share)** and **Withdrawals**.

### The Discrepancy:
- In `walletController.js`, when a user is logged in as a `FULFILLER`, the app is forced to look at the `FULFILLER` wallet.
- However, `initializeTopup` and the Paystack Webhook always credit the `USER` wallet.
- **Result:** An agent can top up ₦10,000, see "Success" in the admin dashboard (which lists all transactions), but their app balance remains ₦0 because they are looking at their empty earnings wallet instead of their funded user wallet.

## 2. Evidence from Source Code

### The Write Side (Webhook):
Always uses `'USER'` role for top-ups:
```javascript
// paymentController.js
const walletId = await walletService.ensureWalletExists(client, 'USER', m.user_id);
```

### The Read Side (App API):
Switches to `'FULFILLER'` role if the user has that role:
```javascript
// walletController.js
if (userRole === 'FULFILLER') {
    const fRes = await db.query("SELECT id FROM fulfillers WHERE user_id = $1", [userId]);
    if (fRes.rows.length > 0) {
        ownerType = 'FULFILLER';
        ownerId = fRes.rows[0].id; // Fulfiller ID, NOT User ID
    }
}
```

## 3. Why the previous fix failed
The previous fix likely focused on refreshing the UI or checking field names, but it didn't address the fact that the money is being stored under a different "Owner ID" entirely.

## 4. Proposed Unification
To resolve this permanently and prevent future "missing money" reports, we must **unify the wallet system**:
- A single wallet per `user_id`.
- `owner_type` should always be `'USER'`.
- All mission settlements, top-ups, and escrow releases should target this single wallet.
- Payouts should be allowed for any user with a balance, provided they have linked bank details (which we already unlocked for fulfillers).

## 5. Affected Transactions Check
The "top-up from several days ago" is likely sitting in the user's `USER` wallet, while they are checking their `FULFILLER` wallet. Unifying the wallets will automatically "reveal" this missing money.
