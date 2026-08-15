exports.up = (pgm) => {
  // 1. Enhance Kitchens Table
  pgm.addColumns('kitchens', {
    user_id: { type: 'integer', references: '"users"', onDelete: 'cascade' },
    description: { type: 'text' },
    logo_url: { type: 'text' },
    state_food_safety_docs: { type: 'jsonb' }, // e.g., { "premises_reg": "url", "health_cert": "url" }
    bank_account_name: { type: 'varchar(255)' },
    bank_account_number: { type: 'varchar(20)' },
    bank_code: { type: 'varchar(10)' },
    pickup_address_id: { type: 'integer', references: '"addresses"' },
  });

  // 2. Enhance Menu Items Table
  pgm.addColumns('menu_items', {
    description: { type: 'text' },
    photo_url: { type: 'text' },
    category: { type: 'varchar(100)', notNull: true, default: 'Main' },
    prep_time_minutes: { type: 'integer', default: 30 },
    modifiers: { type: 'jsonb' }, // e.g., [{"name": "Extra Spicy", "price": 200}]
    created_at: { type: 'timestamp', notNull: true, default: pgm.func('current_timestamp') },
  });

  pgm.createIndex('kitchens', 'user_id');
  pgm.createIndex('menu_items', 'category');
};

exports.down = (pgm) => {
  pgm.dropIndex('menu_items', 'category');
  pgm.dropIndex('kitchens', 'user_id');
  pgm.removeColumns('menu_items', ['description', 'photo_url', 'category', 'prep_time_minutes', 'modifiers', 'created_at']);
  pgm.removeColumns('kitchens', ['user_id', 'description', 'logo_url', 'state_food_safety_docs', 'bank_account_name', 'bank_account_number', 'bank_code', 'pickup_address_id']);
};
