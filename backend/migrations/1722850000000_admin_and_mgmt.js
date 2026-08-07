exports.shorthands = undefined;

exports.up = (pgm) => {
  // Admin Users Table
  pgm.createTable('admin_users', {
    id: 'id',
    username: { type: 'varchar(50)', notNull: true, unique: true },
    password_hash: { type: 'text', notNull: true },
    role: {
      type: 'varchar(20)',
      notNull: true,
      check: "role IN ('super_admin', 'ops', 'finance', 'support', 'analyst')"
    },
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });

  // Zones Table (Spatial pricing zones)
  pgm.createTable('zones', {
    id: 'id',
    name: { type: 'varchar(100)', notNull: true },
    boundary: { type: 'geography(polygon, 4326)', notNull: true },
    base_fare: { type: 'numeric(10, 2)', notNull: true },
    is_active: { type: 'boolean', notNull: true, default: true },
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });
  pgm.createIndex('zones', 'boundary', { method: 'gist' });

  // Disputes Table
  pgm.createTable('disputes', {
    id: 'id',
    order_id: { type: 'integer', notNull: true, references: '"orders"' },
    reporter_id: { type: 'integer', notNull: true, references: '"users"' }, // The one who filed the dispute
    reason: { type: 'text', notNull: true },
    status: {
      type: 'varchar(20)',
      notNull: true,
      default: 'OPEN',
      check: "status IN ('OPEN', 'INVESTIGATING', 'RESOLVED', 'CLOSED')"
    },
    resolution_notes: { type: 'text' },
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });

  // KYC Documents
  pgm.createTable('kyc_documents', {
    id: 'id',
    fulfiller_id: { type: 'integer', notNull: true, references: '"fulfillers"', onDelete: 'cascade' },
    document_type: { type: 'varchar(50)', notNull: true }, // 'ID_CARD', 'DRIVING_LICENSE', etc.
    document_url: { type: 'text', notNull: true },
    status: {
      type: 'varchar(20)',
      notNull: true,
      default: 'PENDING',
      check: "status IN ('PENDING', 'APPROVED', 'REJECTED')"
    },
    rejection_reason: { type: 'text' },
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });

  // Insert an initial super_admin (password should be hashed in production)
  // For alpha/scaffold: 'admin' / 'pikop123'
  pgm.sql("INSERT INTO admin_users (username, password_hash, role) VALUES ('admin', '$2b$10$EixZ9.7vS7mC3kK4z5L4.O.7q/o6Y6y5K0z/o/o6Y6y5K0z/o/o6', 'super_admin')");
};

exports.down = (pgm) => {
  pgm.dropTable('kyc_documents');
  pgm.dropTable('disputes');
  pgm.dropTable('zones');
  pgm.dropTable('admin_users');
};
