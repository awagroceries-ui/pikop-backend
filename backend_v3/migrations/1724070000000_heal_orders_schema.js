exports.up = (pgm) => {
  // Robust idempotent column addition using raw SQL
  const columns = [
    { name: 'recipient_name', type: 'varchar(255)' },
    { name: 'recipient_phone', type: 'varchar(20)' },
    { name: 'notes', type: 'text' },
    { name: 'pickup_display_summary', type: 'text' },
    { name: 'delivery_display_summary', type: 'text' },
    { name: 'item_photo_url', type: 'text' },
    { name: 'payment_method', type: 'varchar(30)' }
  ];

  columns.forEach(col => {
    pgm.sql(`
      DO $$
      BEGIN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='orders' AND column_name='${col.name}') THEN
          ALTER TABLE "orders" ADD COLUMN "${col.name}" ${col.type};
        END IF;
      END $$;
    `);
  });
};

exports.down = (pgm) => {
  pgm.removeColumns('orders', [
    'recipient_name',
    'recipient_phone',
    'notes',
    'pickup_display_summary',
    'delivery_display_summary',
    'item_photo_url',
    'payment_method'
  ]);
};
