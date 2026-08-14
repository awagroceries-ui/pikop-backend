exports.up = (pgm) => {
  // 1. Add read_at to messages
  pgm.addColumns('messages', {
    read_at: { type: 'timestamp', nullable: true },
  });

  // 2. Add last_active_at to users and fulfillers (Prompt 4)
  pgm.addColumns('users', {
    last_active_at: { type: 'timestamp', nullable: true },
  });
  pgm.addColumns('fulfillers', {
    last_active_at: { type: 'timestamp', nullable: true },
  });

  // 3. Rename message content to body (Prompt 1)
  pgm.renameColumn('messages', 'content', 'body');
  // 4. Incident Handling (Prompt 6)
  pgm.addColumns('orders', {
    cancellation_fee_waived: { type: 'boolean', notNull: true, default: false },
    incident_dispute_id: { type: 'uuid', references: '"disputes"', onDelete: 'set null' },
  });
  pgm.addColumns('disputes', {
    category: { type: 'varchar(50)', notNull: true, default: 'general' },
  });
};

exports.down = (pgm) => {
  pgm.renameColumn('messages', 'body', 'content');
  pgm.removeColumns('fulfillers', ['last_active_at']);
  pgm.removeColumns('users', ['last_active_at']);
  pgm.removeColumns('messages', ['read_at']);
  // 4. Incident Handling (Prompt 6)
  pgm.addColumns('orders', {
    cancellation_fee_waived: { type: 'boolean', notNull: true, default: false },
    incident_dispute_id: { type: 'uuid', references: '"disputes"', onDelete: 'set null' },
  });
  pgm.addColumns('disputes', {
    category: { type: 'varchar(50)', notNull: true, default: 'general' },
  });
};
