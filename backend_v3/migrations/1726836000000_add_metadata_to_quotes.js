exports.up = (pgm) => {
  pgm.addColumns('quotes', {
    metadata: { type: 'jsonb' }
  });
};

exports.down = (pgm) => {
  pgm.dropColumns('quotes', ['metadata']);
};
