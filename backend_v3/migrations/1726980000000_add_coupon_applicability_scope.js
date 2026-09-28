exports.up = (pgm) => {
  pgm.sql(`
    ALTER TABLE coupons ADD COLUMN IF NOT EXISTS applicability_scope VARCHAR(50) NOT NULL DEFAULT 'DELIVERY_ONLY';
  `);
};

exports.down = (pgm) => {
  pgm.sql(`
    ALTER TABLE coupons DROP COLUMN IF EXISTS applicability_scope;
  `);
};
