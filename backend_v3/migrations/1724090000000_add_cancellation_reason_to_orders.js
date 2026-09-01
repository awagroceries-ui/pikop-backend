exports.up = (pgm) => {
  pgm.addColumns('orders', {
    cancellation_reason: { type: 'text' }
  });
  pgm.addColumns('fulfillers', {
    profile_photo_url: { type: 'text' }
  });
};

exports.down = (pgm) => {
  pgm.removeColumns('orders', ['cancellation_reason']);
  pgm.removeColumns('fulfillers', ['profile_photo_url']);
};
