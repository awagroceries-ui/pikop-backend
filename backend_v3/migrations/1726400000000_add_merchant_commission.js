exports.up = (pgm) => {
  pgm.addColumns('orders', {
    merchant_commission_amount: { type: 'decimal(12,2)', default: 0.0 }
  });
};

exports.down = (pgm) => {
  pgm.dropColumns('orders', ['merchant_commission_amount']);
};