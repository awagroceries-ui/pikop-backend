exports.up = (pgm) => {
  // 1. Extend Users Role Constraint
  pgm.dropConstraint('users', 'users_role_check');
  pgm.addConstraint('users', 'users_role_check', {
    check: "role IN ('CUSTOMER', 'FULFILLER', 'MERCHANT', 'FLEET_PARTNER', 'ADMIN', 'SUPER_ADMIN')"
  });

  // 2. Fleet Partners Table
  pgm.createTable('fleet_partners', {
    id: 'id',
    user_id: { type: 'integer', notNull: true, references: '"users"', onDelete: 'cascade', unique: true },
    business_name: { type: 'varchar(255)', notNull: true },
    cac_number: { type: 'varchar(50)', unique: true },
    business_address: { type: 'text' },
    fleet_size_estimate: { type: 'integer', default: 0 },
    vehicle_types: { type: 'varchar[]' }, // ['bike', 'car', 'van']
    operating_cities: { type: 'varchar[]' },
    commission_override: { type: 'decimal(5,2)' }, // Pikop share percentage (e.g. 0.20)
    has_overflow_priority: { type: 'boolean', notNull: true, default: false },
    status: { type: 'varchar(50)', notNull: true, default: 'PENDING_VERIFICATION' },
    created_at: { type: 'timestamp', notNull: true, default: pgm.func('current_timestamp') }
  });

  // 3. Fleet Partner Invites (for linking drivers)
  pgm.createTable('fleet_partner_invites', {
    id: 'id',
    fleet_partner_id: { type: 'integer', notNull: true, references: '"fleet_partners"', onDelete: 'cascade' },
    invite_code: { type: 'varchar(20)', notNull: true, unique: true },
    is_active: { type: 'boolean', notNull: true, default: true },
    created_at: { type: 'timestamp', notNull: true, default: pgm.func('current_timestamp') }
  });

  // 4. Link Fulfillers to Fleet Partners
  pgm.addColumns('fulfillers', {
    fleet_partner_id: { type: 'integer', references: '"fleet_partners"', onDelete: 'set null' }
  });

  pgm.createIndex('fleet_partners', 'user_id');
  pgm.createIndex('fleet_partner_invites', 'invite_code');
  pgm.createIndex('fulfillers', 'fleet_partner_id');
};

exports.down = (pgm) => {
  pgm.dropColumns('fulfillers', ['fleet_partner_id']);
  pgm.dropTable('fleet_partner_invites');
  pgm.dropTable('fleet_partners');
};
