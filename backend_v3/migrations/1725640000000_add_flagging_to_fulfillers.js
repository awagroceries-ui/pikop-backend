exports.up = (pgm) => {
  pgm.addColumns('fulfillers', {
    is_flagged: { type: 'boolean', notNull: true, default: false },
    flag_reason: { type: 'text' },
    last_tier_audit_at: { type: 'timestamp' }
  });

  pgm.createIndex('fulfillers', 'is_flagged');
};

exports.down = (pgm) => {
  pgm.dropColumns('fulfillers', ['last_tier_audit_at', 'flag_reason', 'is_flagged']);
};
