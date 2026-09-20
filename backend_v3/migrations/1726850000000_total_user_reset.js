exports.up = (pgm) => {
  // Destructive Wipe: Removing all user-generated data and non-admin accounts

  // 1. Transactional & Log Data
  pgm.sql("DELETE FROM wallet_ledger_entries");
  pgm.sql("DELETE FROM order_items");
  pgm.sql("DELETE FROM orders");
  pgm.sql("DELETE FROM quotes");
  pgm.sql("DELETE FROM disputes");
  pgm.sql("DELETE FROM returns");
  pgm.sql("DELETE FROM emergency_alerts");
  pgm.sql("DELETE FROM sms_logs");
  pgm.sql("DELETE FROM fcm_logs");
  pgm.sql("DELETE FROM audit_logs");
  pgm.sql("DELETE FROM landmark_suggestions");
  pgm.sql("DELETE FROM expansion_waitlist");

  // 2. Profile & Entity Data
  pgm.sql("DELETE FROM kyc_documents");
  pgm.sql("DELETE FROM fulfillers");
  pgm.sql("DELETE FROM menu_items");
  pgm.sql("DELETE FROM products");
  pgm.sql("DELETE FROM kitchens");
  pgm.sql("DELETE FROM vendors");
  pgm.sql("DELETE FROM corporate_sub_accounts");
  pgm.sql("DELETE FROM corporate_accounts");
  pgm.sql("DELETE FROM merchant_sub_accounts");
  pgm.sql("DELETE FROM merchant_accounts");
  pgm.sql("DELETE FROM wallets WHERE owner_type != 'PLATFORM'");

  // 3. User Identity & Sessions
  pgm.sql("DELETE FROM user_sessions");
  pgm.sql("DELETE FROM otp_verifications");
  pgm.sql("DELETE FROM referrals");
  pgm.sql("DELETE FROM loyalty_ledger");

  // 4. Wipe non-admin users
  // Important: Preserve ADMIN and SUPER_ADMIN roles so dashboard remains accessible
  pgm.sql("DELETE FROM users WHERE role NOT IN ('ADMIN', 'SUPER_ADMIN')");
};

exports.down = (pgm) => {
  // Reset is irreversible, no-op for down
};
