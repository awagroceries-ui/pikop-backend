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

  // Note: cancellation_fee_waived and incident_dispute_id were already added in 1722950000000_order_lifecycle_ext.js
  // We just ensure category is available in disputes if not already there or needs update
  // Since 1722950000000 added it with default 'other', we might just leave it.
};

exports.down = (pgm) => {
  pgm.renameColumn('messages', 'body', 'content');
  pgm.removeColumns('fulfillers', ['last_active_at']);
  pgm.removeColumns('users', ['last_active_at']);
  pgm.removeColumns('messages', ['read_at']);
};
