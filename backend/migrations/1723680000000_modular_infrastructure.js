exports.up = (pgm) => {
  // 1. Knowledge Base Table
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
  });

  // 2. Add module_type to Orders
  pgm.addColumns('orders', {
    module_type: {
      type: 'varchar(50)',
      notNull: true,
      default: 'DELIVERY'
      // Options: DELIVERY, MARKETPLACE, FOODS
    },
  });

  pgm.createIndex('knowledge_base', ['target_audience', 'category']);
  pgm.createIndex('orders', 'module_type');
};

exports.down = (pgm) => {
  pgm.removeColumns('orders', ['module_type']);
  pgm.dropTable('knowledge_base');
};
