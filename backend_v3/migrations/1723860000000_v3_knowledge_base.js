exports.up = (pgm) => {
  pgm.createTable('knowledge_base', {
    id: 'id',
    title: { type: 'varchar(255)', notNull: true },
    content: { type: 'text', notNull: true },
    category: { type: 'varchar(100)', notNull: true },
    target_audience: {
      type: 'varchar(20)',
      notNull: true,
      default: 'BOTH',
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

  pgm.createIndex('knowledge_base', ['target_audience', 'category']);
};

exports.down = (pgm) => {
  pgm.dropTable('knowledge_base');
};
