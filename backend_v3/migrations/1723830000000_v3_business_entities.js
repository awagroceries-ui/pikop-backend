exports.up = (pgm) => {
  // 1. Vendors (Marketplace)
  pgm.createTable('vendors', {
    id: { type: 'uuid', primaryKey: true, default: pgm.func('gen_random_uuid()') },
    business_name: { type: 'varchar(255)', notNull: true },
    cac_number: { type: 'varchar(50)', unique: true },
    contact_email: { type: 'varchar(255)', notNull: true },
    status: { type: 'varchar(20)', notNull: true, default: 'pending' },
    city: { type: 'varchar(50)' },
    pickup_address_id: { type: 'integer', references: '"addresses"' },
    created_at: { type: 'timestamp', default: pgm.func('current_timestamp') },
  });

  // 2. Kitchens (Foods & Meals)
  pgm.createTable('kitchens', {
    id: { type: 'uuid', primaryKey: true, default: pgm.func('gen_random_uuid()') },
    business_name: { type: 'varchar(255)', notNull: true },
    cac_number: { type: 'varchar(50)', unique: true },
    contact_email: { type: 'varchar(255)', notNull: true },
    status: { type: 'varchar(20)', notNull: true, default: 'pending' },
    city: { type: 'varchar(50)' },
    cuisine_type: { type: 'varchar(100)' },
    created_at: { type: 'timestamp', default: pgm.func('current_timestamp') },
  });

  // 3. Products & Menu Items
  pgm.createTable('products', {
    id: 'id',
    vendor_id: { type: 'uuid', references: '"vendors"', onDelete: 'cascade' },
    name: { type: 'varchar(255)', notNull: true },
    price: { type: 'decimal(12,2)', notNull: true },
    stock_quantity: { type: 'integer', default: 0 },
    active: { type: 'boolean', default: true },
  });

  pgm.createTable('menu_items', {
    id: 'id',
    kitchen_id: { type: 'uuid', references: '"kitchens"', onDelete: 'cascade' },
    name: { type: 'varchar(255)', notNull: true },
    price: { type: 'decimal(12,2)', notNull: true },
    available: { type: 'boolean', default: true },
  });
};

exports.down = (pgm) => {
  pgm.dropTable('menu_items');
  pgm.dropTable('products');
  pgm.dropTable('kitchens');
  pgm.dropTable('vendors');
};
