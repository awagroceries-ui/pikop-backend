exports.up = (pgm) => {
  pgm.addColumns('orders', {
    vendor_id: { type: 'uuid', references: '"vendors"', onDelete: 'set null' },
    kitchen_id: { type: 'uuid', references: '"kitchens"', onDelete: 'set null' },
    product_id: { type: 'integer', references: '"products"', onDelete: 'set null' },
    menu_item_id: { type: 'integer', references: '"menu_items"', onDelete: 'set null' },
  });

  pgm.createIndex('orders', 'product_id');
  pgm.createIndex('orders', 'menu_item_id');
  pgm.createIndex('orders', 'vendor_id');
  pgm.createIndex('orders', 'kitchen_id');
};

exports.down = (pgm) => {
  pgm.dropColumns('orders', ['menu_item_id', 'product_id', 'kitchen_id', 'vendor_id']);
};
