# Task Checklist: Delivery Fee Audit & Payout Decoupling

- `[/]` **Audit Findings**
  - `[x]` Confirm 75/25 split is universal (Standalone + Marketplace).
  - `[x]` Confirm split rate is admin-configurable.
  - `[x]` Confirm Fulfiller payout is decoupled in OTP flow.
  - `[x]` Identify gap in manual/admin status update flow.
- `[x]` **Implementation Fixes**
  - `[x]` Modify `updateStatus` in `orderController.js` to trigger settlement for all DELIVERED orders.
  - `[x]` Modify `updateOrderStatus` in `adminController.js` to trigger settlement for all DELIVERED orders.
- `[ ]` **Verification**
  - `[ ]` Confirm fulfiller payout on manual completion of COD/Marketplace orders.
  - `[ ]` Git commit and push changes.
