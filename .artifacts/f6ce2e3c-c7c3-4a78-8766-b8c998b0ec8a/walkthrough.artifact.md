# Walkthrough - Standardized Professional Legal Overhaul

I have completely overhauled the platform's legal documents (Terms & Conditions and Privacy Policy) to meet industry standards (e.g., Bolt/Uber), ensuring maximum protection for Awa Foods & Groceries while providing operational clarity to all users.

## Legal Framework Improvements

### 1. "Commercial Agent" Model
- **Definition:** Legally defines Pikop as a technology intermediary and a commercial agent for independent Fulfillers.
- **Payment Clarity:** Explicitly states that once a customer pays Pikop, their legal obligation to pay the independent courier is satisfied.

### 2. Comprehensive Liability Protection
- **Liability Cap:** Implemented a standard clause capping total platform liability at the amount of fees paid by the user in the preceding 3 months.
- **Hold Harmless:** Strengthened the indemnification language. Users and Fulfillers agree to protect Pikop from any legal claims arising from their misuse of the service or the transport of illegal items.

### 3. Prohibited Items Discovery & Enforcement
- **Reporting:** Added a strict clause: *"Discovery of prohibited items will result in immediate reporting of the Sender and the item to the police."*
- **Disposal:** *"Prohibited items will be discarded or surrendered to authorities immediately without refund or liability."*

### 4. Refined Financial Policies
- **25% Cancellation Fee:** Applied if an agent is matched but pickup hasn't occurred.
- **No Cancellation:** Strictly prohibited once an item is picked up.
- **75% Return Fee:** Applied for failed missions requiring return to the sender.
- **Refund as Credit:** All approved refunds are now officially issued as **Pikop Wallet Credits**, protecting platform cash flow.

### 5. Email & Privacy Alignment
- **NDPA Compliance:** Updated the Privacy Policy to align with the **Nigeria Data Protection Act (NDPA)**.
- **Welcome Emails:** Synchronized the policy summary in all welcome emails to reflect these new standardized rules and the **Port Harcourt** hub location.

## Verification Results

### Backend Implementation
- Updated `legalController.js` and `emailService.js`.
- Verified all HTML rendering for mobile app compatibility.
- **Result:** `PASS`.

## Deployment Instructions (VPS)
Please apply these legal overhauls to your **VPS**:

```bash
cd /var/www/pikop-api/backend_v3/backend_v3
git pull origin main
pm2 restart pikop-v3
```
