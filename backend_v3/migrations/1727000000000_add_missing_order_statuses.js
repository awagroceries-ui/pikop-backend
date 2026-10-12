// v4.5 fix: orders.createOrder() sets status = 'PENDING_ACKNOWLEDGMENT' whenever
// the recipient is an app user or recipient-pays (isReceiverAppUser), but that
// value was never added to orders_status_check (only 'SCHEDULED' was patched in
// previously, in 1726940000000). The result: order creation for that entire class
// of deliveries was failing outright with a DB constraint violation ("new row for
// relation orders violates check constraint orders_status_check"), so the order
// never got created at all -- found by reproducing issues #10/#11 end-to-end
// against a clean database and hitting this error directly.
//
// Also adding 'ARRIVED_AT_PICKUP' here: the generic PATCH /orders/:id/status
// endpoint has no DB-level support for it either, and it's needed to round out
// the explicit mission-status machine (accepted -> en route to pickup ->
// arrived at pickup -> picked up -> en route to destination -> arrived at
// destination -> delivered) requested alongside the real-time tracking fixes.
exports.up = (pgm) => {
  pgm.sql(`
    ALTER TABLE "orders" DROP CONSTRAINT IF EXISTS "orders_status_check";
    ALTER TABLE "orders" ADD CONSTRAINT "orders_status_check"
    CHECK (status IN (
      'SEARCHING', 'MATCHED', 'QUEUED', 'PICKED_UP', 'IN_TRANSIT',
      'ARRIVED_AT_PICKUP', 'ARRIVED_AT_DELIVERY', 'DELIVERED', 'CANCELLED',
      'RECIPIENT_ABSENT', 'PAYMENT_PENDING', 'PAYMENT_CAPTURED',
      'DELIVERED_PENDING_CONFIRMATION', 'CONFIRMED', 'DISPUTED', 'REFUNDED',
      'RELEASED', 'SCHEDULED', 'PENDING', 'PROCESSING', 'DISPATCHED',
      'OUT_FOR_DELIVERY', 'PENDING_ACKNOWLEDGMENT'
    ));
  `);
};

exports.down = (pgm) => {
  pgm.sql(`
    ALTER TABLE "orders" DROP CONSTRAINT IF EXISTS "orders_status_check";
    ALTER TABLE "orders" ADD CONSTRAINT "orders_status_check"
    CHECK (status IN (
      'SEARCHING', 'MATCHED', 'QUEUED', 'PICKED_UP', 'IN_TRANSIT',
      'ARRIVED_AT_DELIVERY', 'DELIVERED', 'CANCELLED', 'RECIPIENT_ABSENT',
      'PAYMENT_PENDING', 'PAYMENT_CAPTURED', 'DELIVERED_PENDING_CONFIRMATION',
      'CONFIRMED', 'DISPUTED', 'REFUNDED', 'RELEASED', 'SCHEDULED', 'PENDING',
      'PROCESSING', 'DISPATCHED', 'OUT_FOR_DELIVERY'
    ));
  `);
};
