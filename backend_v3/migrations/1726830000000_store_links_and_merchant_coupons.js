exports.up = (pgm) => {
  // 1. Add store_slug to vendors and kitchens
  pgm.addColumns('vendors', {
    store_slug: { type: 'varchar(100)', unique: true }
  });
  pgm.addColumns('kitchens', {
    store_slug: { type: 'varchar(100)', unique: true }
  });

  // 2. Add merchant_id to coupons
  // merchant_id can refer to either a vendor or a kitchen
  pgm.addColumns('coupons', {
    merchant_id: { type: 'uuid', references: 'vendors', onDelete: 'cascade' },
    kitchen_id: { type: 'uuid', references: 'kitchens', onDelete: 'cascade' }
  });

  pgm.createIndex('vendors', 'store_slug');
  pgm.createIndex('kitchens', 'store_slug');
  pgm.createIndex('coupons', 'merchant_id');
  pgm.createIndex('coupons', 'kitchen_id');
};

exports.down = (pgm) => {
  pgm.dropColumns('coupons', ['merchant_id', 'kitchen_id']);
  pgm.dropColumns('kitchens', ['store_slug']);
  pgm.dropColumns('vendors', ['store_slug']);
};
