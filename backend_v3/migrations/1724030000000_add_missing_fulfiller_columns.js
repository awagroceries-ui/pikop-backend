exports.up = (pgm) => {
  pgm.addColumns('fulfillers', {
    mobility_type: { type: 'varchar(50)' },
    registration_number: { type: 'varchar(50)' },
    make: { type: 'varchar(50)' },
    model: { type: 'varchar(50)' },
    color: { type: 'varchar(30)' },
    kyc_verified_at: { type: 'timestamp' },
    kyc_provider_ref: { type: 'varchar(255)' }
  });
};

exports.down = (pgm) => {
  pgm.dropColumns('fulfillers', [
    'mobility_type',
    'registration_number',
    'make',
    'model',
    'color',
    'kyc_verified_at',
    'kyc_provider_ref'
  ]);
};
