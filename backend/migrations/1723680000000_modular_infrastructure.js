exports.up = (pgm) => {
  // 1. Knowledge Base Table (Safe Create)
  pgm.createTable('knowledge_base', {
    id: 'id',
    title: { type: 'varchar(255)', notNull: true },
    content: { type: 'text', notNull: true },
    category: { type: 'varchar(100)', notNull: true },
    target_audience: {
      type: 'varchar(20)',
      notNull: true,
      check: "target_audience IN ('CUSTOMER', 'FULFILLER', 'BOTH')"
    },
    priority: { type: 'integer', notNull: true, default: 0 },
    is_active: { type: 'boolean', notNull: true, default: true },
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  }, { ifNotExists: true });

  // 2. Add module_type to Orders (Safe Add)
  // node-pg-migrate addColumns doesn't have ifNotExists, so we use SQL
  pgm.sql(`
    DO $$
    BEGIN
      IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='orders' AND column_name='module_type') THEN
        ALTER TABLE orders ADD COLUMN module_type varchar(50) NOT NULL DEFAULT 'DELIVERY';
      END IF;
    END $$;
  `);

  pgm.createIndex('knowledge_base', ['target_audience', 'category'], { ifNotExists: true });
  pgm.createIndex('orders', 'module_type', { ifNotExists: true });
};

exports.down = (pgm) => {
  pgm.removeColumns('orders', ['module_type']);
  pgm.dropTable('knowledge_base');
};
