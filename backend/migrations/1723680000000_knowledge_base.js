exports.up = (pgm) => {
  pgm.createTable('knowledge_base', {
    id: { type: 'uuid', primaryKey: true, default: pgm.func('gen_random_uuid()') },
    title: { type: 'varchar(255)', notNull: true },
    content: { type: 'text', notNull: true },
    category: { type: 'varchar(100)', notNull: true }, // General, Payments, Deliveries, Account
    target_audience: {
      type: 'varchar(20)',
      notNull: true,
      check: "target_audience IN ('CUSTOMER', 'FULFILLER', 'BOTH')"
    },
    priority: { type: 'integer', notNull: true, default: 0 },
    is_active: { type: 'boolean', notNull: true, default: true },
    created_at: { type: 'timestamp', notNull: true, default: pgm.func('current_timestamp') },
    updated_at: { type: 'timestamp', notNull: true, default: pgm.func('current_timestamp') }
  });

  pgm.createIndex('knowledge_base', ['category', 'target_audience']);
};

exports.down = (pgm) => {
  pgm.dropTable('knowledge_base');
};
