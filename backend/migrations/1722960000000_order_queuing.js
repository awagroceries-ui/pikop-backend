exports.up = (pgm) => {
  pgm.addColumns('orders', {
    queued_for_fulfiller_id: { type: 'integer', references: '"fulfillers"', onDelete: 'set null' },
  });

  // Note: 'QUEUED' is handled as a varchar value in the status column.
  // No explicit enum update needed if using varchar checks.
};

exports.down = (pgm) => {
  pgm.removeColumns('orders', ['queued_for_fulfiller_id']);
};
