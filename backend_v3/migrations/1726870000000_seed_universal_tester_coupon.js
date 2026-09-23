exports.up = (pgm) => {
  // Seed 100% universal tester coupon for bypass testing across all modules
  pgm.sql(`
    INSERT INTO coupons (code, discount_type, discount_value, min_order_amount, usage_limit, is_active)
    VALUES ('TESTER100', 'PERCENTAGE', 100.00, 0.00, 999999, true)
    ON CONFLICT (code) DO UPDATE
    SET discount_type = 'PERCENTAGE', discount_value = 100.00, is_active = true;
  `);
};

exports.down = (pgm) => {
  pgm.sql("DELETE FROM coupons WHERE code = 'TESTER100'");
};
