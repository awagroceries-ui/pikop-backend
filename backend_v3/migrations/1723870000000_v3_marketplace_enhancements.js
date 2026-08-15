exports.up = (pgm) => {
  // 1. Enhance Vendors Table
  pgm.addColumns('vendors', {
    logo_url: { type: 'text' },
    business_hours: { type: 'jsonb' }, // e.g. { "mon": "08:00-18:00", ... }
    description: { type: 'text' },
    bank_account_name: { type: 'varchar(255)' },
    bank_account_number: { type: 'varchar(20)' },
    bank_code: { type: 'varchar(10)' },
    user_id: { type: 'integer', references: '"users"', onDelete: 'cascade' }, // Owner
  });

  // 2. Enhance Products Table
  pgm.addColumns('products', {
    description: { type: 'text' },
    category: { type: 'varchar(100)', notNull: true, default: 'General' },
    photo_url: { type: 'text' },
    unit: { type: 'varchar(50)', default: 'item' }, // e.g. piece, kg, pack
    nafdac_number: { type: 'varchar(50)' },
    created_at: { type: 'timestamp', notNull: true, default: pgm.func('current_timestamp') },
  });

  pgm.createIndex('products', 'category');
  pgm.createIndex('vendors', 'user_id');
};

exports.down = (pgm) => {
  pgm.dropIndex('vendors', 'user_id');
  pgm.dropIndex('products', 'category');
  pgm.removeColumns('products', ['description', 'category', 'photo_url', 'unit', 'nafdac_number', 'created_at']);
  pgm.removeColumns('vendors', ['logo_url', 'business_hours', 'description', 'bank_account_name', 'bank_account_number', 'bank_code', 'user_id']);
};
