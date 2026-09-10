exports.up = (pgm) => {
  pgm.sql(`
    ALTER TABLE "quotes" ADD COLUMN IF NOT EXISTS "required_fulfiller_classes" text[] DEFAULT ARRAY['agent', 'rider', 'driver']::text[];
    ALTER TABLE "orders" ADD COLUMN IF NOT EXISTS "required_fulfiller_classes" text[] DEFAULT ARRAY['agent', 'rider', 'driver']::text[];
  `);
};

exports.down = (pgm) => {
  pgm.dropColumns('orders', ['required_fulfiller_classes']);
  pgm.dropColumns('quotes', ['required_fulfiller_classes']);
};
