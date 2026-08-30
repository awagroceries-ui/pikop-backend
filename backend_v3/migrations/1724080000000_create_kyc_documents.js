exports.up = (pgm) => {
  pgm.createTable('kyc_documents', {
    id: 'id',
    fulfiller_id: {
      type: 'integer',
      notNull: true,
      references: '"fulfillers"',
      onDelete: 'cascade',
    },
    doc_type: { type: 'varchar(50)', notNull: true },
    file_url: { type: 'text', notNull: true },
    status: { type: 'varchar(20)', notNull: true, default: 'PENDING' },
    expiry_date: { type: 'timestamp' },
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });

  pgm.createIndex('kyc_documents', 'fulfiller_id');
};

exports.down = (pgm) => {
  pgm.dropTable('kyc_documents');
};
