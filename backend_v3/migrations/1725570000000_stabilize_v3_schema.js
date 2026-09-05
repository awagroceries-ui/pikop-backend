exports.up = (pgm) => {
  // Add missing columns to orders table for POD and timestamps
  pgm.sql(`
    DO $$
    BEGIN
      IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='orders' AND column_name='pod_photo_url') THEN
        ALTER TABLE "orders" ADD COLUMN "pod_photo_url" TEXT;
      END IF;
      IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='orders' AND column_name='delivered_at') THEN
        ALTER TABLE "orders" ADD COLUMN "delivered_at" TIMESTAMP;
      END IF;
    END $$;
  `);

  // Add kyc_details to fulfillers to store full reports from providers
  pgm.sql(`
    DO $$
    BEGIN
      IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='fulfillers' AND column_name='kyc_details') THEN
        ALTER TABLE "fulfillers" ADD COLUMN "kyc_details" JSONB;
      END IF;
      -- Ensure kyc_verification_status exists if we intend to use it,
      -- but usually v3 uses didit_verification_status.
      -- We will keep didit_verification_status as the primary one.
    END $$;
  `);
};

exports.down = (pgm) => {
  pgm.removeColumns('orders', ['pod_photo_url', 'delivered_at']);
  pgm.removeColumns('fulfillers', ['kyc_details']);
};
