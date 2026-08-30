exports.up = (pgm) => {
  pgm.addColumns('orders', {
    recipient_name: { type: 'varchar(255)' },
    recipient_phone: { type: 'varchar(20)' },
    notes: { type: 'text' },
    pickup_display_summary: { type: 'text' },
    delivery_display_summary: { type: 'text' },
    item_photo_url: { type: 'text' },
    payment_method: { type: 'varchar(30)' },
    parent_order_id: { type: 'integer', references: '"orders"', onDelete: 'set null' }
  });
};

exports.down = (pgm) => {
  pgm.removeColumns('orders', [
    'recipient_name',
    'recipient_phone',
    'notes',
    'pickup_display_summary',
    'delivery_display_summary',
    'item_photo_url',
    'payment_method',
    'parent_order_id'
  ]);
};
