exports.up = (pgm) => {
  pgm.createTable('order_status_history', {
    id: 'id',
    order_id: { type: 'integer', notNull: true, references: '"orders"', onDelete: 'cascade' },
    status: { type: 'varchar(50)', notNull: true },
    description: { type: 'text' },
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });
  pgm.createIndex('order_status_history', 'order_id');
};

exports.down = (pgm) => {
  pgm.dropTable('order_status_history');
};
