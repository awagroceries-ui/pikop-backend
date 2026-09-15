exports.up = (pgm) => {
  // Extend status column length for all key entities to accommodate longer descriptive statuses
  pgm.alterColumn('vendors', 'status', { type: 'varchar(50)' });
  pgm.alterColumn('kitchens', 'status', { type: 'varchar(50)' });
  pgm.alterColumn('users', 'status', { type: 'varchar(50)' });
  pgm.alterColumn('fulfillers', 'status', { type: 'varchar(50)' });
};

exports.down = (pgm) => {
  // Reverting to original length might truncate data if longer statuses exist
  pgm.alterColumn('vendors', 'status', { type: 'varchar(20)' });
  pgm.alterColumn('kitchens', 'status', { type: 'varchar(20)' });
  pgm.alterColumn('users', 'status', { type: 'varchar(20)' });
  pgm.alterColumn('fulfillers', 'status', { type: 'varchar(20)' });
};