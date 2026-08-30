exports.up = (pgm) => {
  pgm.sql(`
    CREATE TABLE IF NOT EXISTS "kyc_documents" (
      "id" serial PRIMARY KEY,
      "fulfiller_id" integer NOT NULL REFERENCES "fulfillers" ON DELETE cascade,
      "doc_type" varchar(50) NOT NULL,
      "file_url" text NOT NULL,
      "status" varchar(20) DEFAULT 'PENDING' NOT NULL,
      "expiry_date" timestamp,
      "created_at" timestamp DEFAULT current_timestamp NOT NULL
    );
  `);

  pgm.sql(`
    DO $$
    BEGIN
      IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'kyc_documents_fulfiller_id_idx') THEN
        CREATE INDEX "kyc_documents_fulfiller_id_idx" ON "kyc_documents" ("fulfiller_id");
      END IF;
    END $$;
  `);
};

exports.down = (pgm) => {
  pgm.dropTable('kyc_documents');
};
