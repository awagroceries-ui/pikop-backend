exports.up = (pgm) => {
  // 1. Vendors Table (Marketplace)
  pgm.createTable('vendors', {
    id: { type: 'uuid', primaryKey: true, default: pgm.func('gen_random_uuid()') },
    business_name: { type: 'varchar(255)', notNull: true },
    cac_number: { type: 'varchar(50)', unique: true },
    cac_verified: { type: 'boolean', default: false },
    contact_email: { type: 'varchar(255)', notNull: true },
    bank_account_id: { type: 'uuid' },
    status: { type: 'varchar(20)', notNull: true, default: 'pending' },
    city: { type: 'varchar(50)' },
    pickup_address_id: { type: 'integer', references: '"addresses"' },
    created_at: { type: 'timestamp', default: pgm.func('current_timestamp') },
    approved_at: { type: 'timestamp' },
  });

  // 2. Kitchens Table (Foods & Meals)
  pgm.createTable('kitchens', {
    id: { type: 'uuid', primaryKey: true, default: pgm.func('gen_random_uuid()') },
    business_name: { type: 'varchar(255)', notNull: true },
    cac_number: { type: 'varchar(50)', unique: true },
    cac_verified: { type: 'boolean', default: false },
    contact_email: { type: 'varchar(255)', notNull: true },
    bank_account_id: { type: 'uuid' },
    status: { type: 'varchar(20)', notNull: true, default: 'pending' },
    city: { type: 'varchar(50)' },
    state_food_safety_docs: { type: 'jsonb' },
    cuisine_type: { type: 'varchar(100)' },
    avg_prep_time_minutes: { type: 'integer', default: 30 },
    created_at: { type: 'timestamp', default: pgm.func('current_timestamp') },
    approved_at: { type: 'timestamp' },
  });

  // 3. Products & Menu Items
  pgm.createTable('products', {
    id: 'id',
    vendor_id: { type: 'uuid', references: '"vendors"', onDelete: 'cascade' },
    name: { type: 'varchar(255)', notNull: true },
    category: { type: 'varchar(100)' },
    price: { type: 'decimal(12,2)', notNull: true },
    stock_quantity: { type: 'integer', default: 0 },
    unit: { type: 'varchar(50)' },
    photo_url: { type: 'text' },
    nafdac_number: { type: 'varchar(50)' },
    active: { type: 'boolean', default: true },
  });

  pgm.createTable('menu_items', {
    id: 'id',
    kitchen_id: { type: 'uuid', references: '"kitchens"', onDelete: 'cascade' },
    name: { type: 'varchar(255)', notNull: true },
    description: { type: 'text' },
    category: { type: 'varchar(100)' },
    price: { type: 'decimal(12,2)', notNull: true },
    prep_time_minutes: { type: 'integer' },
    photo_url: { type: 'text' },
    modifiers: { type: 'jsonb' },
    available: { type: 'boolean', default: true },
  });
};

exports.down = (pgm) => {
  pgm.dropTable('menu_items');
  pgm.dropTable('products');
  pgm.dropTable('kitchens');
  pgm.dropTable('vendors');
};
