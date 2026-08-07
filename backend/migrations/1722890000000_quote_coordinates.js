exports.up = (pgm) => {
  pgm.addColumns('quotes', {
    pickup_location: { type: 'geography(point, 4326)' },
    delivery_location: { type: 'geography(point, 4326)' },
  });
};

exports.down = (pgm) => {
  pgm.removeColumns('quotes', ['pickup_location', 'delivery_location']);
};
