exports.up = (pgm) => {
  pgm.addColumns('fulfillers', {
    bank_name: { type: 'varchar(100)' },
    account_number: { type: 'varchar(20)' },
    paystack_recipient_code: { type: 'varchar(100)' },
  });
};

exports.down = (pgm) => {
  pgm.removeColumns('fulfillers', ['bank_name', 'account_number', 'paystack_recipient_code']);
};
