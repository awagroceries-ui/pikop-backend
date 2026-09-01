exports.up = (pgm) => {
  pgm.addColumns('orders', {
    coupon_id: { type: 'uuid', references: '"coupons"', onDelete: 'set null' }
  });
};

exports.down = (pgm) => {
  pgm.removeColumns('orders', ['coupon_id']);
};
