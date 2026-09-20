exports.up = (pgm) => {
  pgm.createTable('order_items', {
    id: 'id',
    order_id: { type: 'integer', notNull: true, references: '"orders"', onDelete: 'cascade' },
    product_id: { type: 'uuid', references: '"products"', onDelete: 'set null' },
    menu_item_id: { type: 'uuid', references: '"menu_items"', onDelete: 'set null' },
    name: { type: 'varchar(255)', notNull: true },
    quantity: { type: 'integer', notNull: true, default: 1 },
    unit_price: { type: 'decimal(12,2)', notNull: true },
    total_price: { type: 'decimal(12,2)', notNull: true }
  });

  pgm.createIndex('order_items', 'order_id');
};

exports.down = (pgm) => {
  pgm.dropTable('order_items');
};
