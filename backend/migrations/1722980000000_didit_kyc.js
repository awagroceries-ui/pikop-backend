exports.up = (pgm) => {
  pgm.addColumns('fulfillers', {
    didit_session_id: { type: 'varchar(255)', nullable: true },
    didit_verification_status: {
      type: 'varchar(50)',
      notNull: true,
      default: 'not_started'
      // not_started, pending, approved, declined, needs_review
    },
    didit_verified_at: { type: 'timestamp', nullable: true },
  });
};

exports.down = (pgm) => {
  pgm.removeColumns('fulfillers', ['didit_session_id', 'didit_verification_status', 'didit_verified_at']);
};
