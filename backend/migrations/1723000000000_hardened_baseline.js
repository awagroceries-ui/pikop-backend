exports.up = (pgm) => {
  // 1. Hardened Verification Codes (Transitioning to hashes)
  pgm.renameColumn('orders', 'pickup_code', 'pickup_code_hash');
  pgm.renameColumn('orders', 'delivery_code', 'delivery_code_hash');
  pgm.alterColumn('orders', 'pickup_code_hash', { type: 'text' });
  pgm.alterColumn('orders', 'delivery_code_hash', { type: 'text' });

  // 2. Proof-of-Delivery Metadata
  pgm.addColumns('orders', {
    capture_lat: { type: 'numeric(10, 7)', nullable: true },
    capture_lng: { type: 'numeric(10, 7)', nullable: true },
    capture_timestamp: { type: 'timestamp', nullable: true },
    eligible_classes: { type: 'varchar[]', nullable: true }, // Array of classes: ['agent', 'rider']
  });

  // 3. User & Fulfiller FCM Token columns (Hardening)
  // These already exist in fcm_tokens table, but we ensure the link is solid
  pgm.createIndex('orders', 'eligible_classes', { method: 'gin' });
};

exports.down = (pgm) => {
  pgm.removeColumns('orders', ['capture_lat', 'capture_lng', 'capture_timestamp', 'eligible_classes']);
  pgm.renameColumn('orders', 'pickup_code_hash', 'pickup_code');
  pgm.renameColumn('orders', 'delivery_code_hash', 'delivery_code');
};
