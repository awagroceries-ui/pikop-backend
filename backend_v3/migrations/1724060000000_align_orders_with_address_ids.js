exports.up = (pgm) => {
  pgm.addColumns('orders', {
    pickup_address_id: { type: 'integer', references: '"addresses"', onDelete: 'set null' },
    delivery_address_id: { type: 'integer', references: '"addresses"', onDelete: 'set null' },
    item_description: { type: 'text' },
    size_tier: { type: 'varchar(20)' },
    payment_channel: { type: 'varchar(50)' },
    pickup_code_hash: { type: 'text' },
    delivery_code_hash: { type: 'text' }
  });
};

exports.down = (pgm) => {
  pgm.dropColumns('orders', [
    'pickup_address_id',
    'delivery_address_id',
    'size_tier',
    'payment_channel',
    'pickup_code_hash',
    'delivery_code_hash'
  ]);
};
