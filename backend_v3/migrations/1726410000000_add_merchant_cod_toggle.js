exports.up = (pgm) => {
  pgm.addColumns('vendors', {
    accepts_cod: { type: 'boolean', default: true }
  });
  pgm.addColumns('kitchens', {
    accepts_cod: { type: 'boolean', default: true }
  });
};

exports.down = (pgm) => {
  pgm.dropColumns('vendors', ['accepts_cod']);
  pgm.dropColumns('kitchens', ['accepts_cod']);
};