exports.up = (pgm) => {
  pgm.createTable('quotes', {
    id: { type: 'uuid', primaryKey: true, default: pgm.func('gen_random_uuid()') },
    user_id: { type: 'integer', references: '"users"' },
    pickup_address: { type: 'text', notNull: true },
    delivery_address: { type: 'text', notNull: true },
    item_description: { type: 'text', notNull: true },
    size_tier: { type: 'varchar(20)', notNull: true },
    total_fare: { type: 'numeric(10, 2)', notNull: true },
    confidence_score: { type: 'numeric(3, 2)' },
    expires_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func("current_timestamp + interval '15 minutes'")
    },
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });
};

exports.down = (pgm) => {
  pgm.dropTable('quotes');
};
