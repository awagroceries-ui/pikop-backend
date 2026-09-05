exports.up = (pgm) => {
  pgm.createTable('disputes', {
    id: 'id',
    order_id: { type: 'integer', notNull: true, references: '"orders"' },
    reporter_id: { type: 'integer', notNull: true, references: '"users"' },
    reason: { type: 'text', notNull: true },
    status: {
      type: 'varchar(20)',
      notNull: true,
      default: 'OPEN',
      check: "status IN ('OPEN', 'INVESTIGATING', 'RESOLVED', 'CLOSED')"
    },
    resolution_notes: { type: 'text' },
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });

  pgm.createIndex('disputes', 'order_id');
};

exports.down = (pgm) => {
  pgm.dropTable('disputes');
};
