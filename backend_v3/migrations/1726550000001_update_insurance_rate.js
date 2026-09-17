exports.up = (pgm) => {
  // Update insurance rate from 1% to 4%
  pgm.sql(`
    UPDATE settings
    SET value = '0.04'
    WHERE key = 'insurance_rate';
  `);
};

exports.down = (pgm) => {
  pgm.sql(`
    UPDATE settings
    SET value = '0.01'
    WHERE key = 'insurance_rate';
  `);
};
