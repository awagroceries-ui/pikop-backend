exports.up = (pgm) => {
  pgm.addColumns('vendors', {
    category: { type: 'string', default: 'Shop' }
  });
  pgm.addColumns('kitchens', {
    category: { type: 'string', default: 'Food' }
  });
};

exports.down = (pgm) => {
  pgm.dropColumns('vendors', ['category']);
  pgm.dropColumns('kitchens', ['category']);
};