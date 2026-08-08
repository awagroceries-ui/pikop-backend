exports.up = (pgm) => {
  // 1. Conversations Table
  pgm.createTable('conversations', {
    id: { type: 'uuid', primaryKey: true, default: pgm.func('gen_random_uuid()') },
    participant_type: { type: 'varchar(20)', notNull: true }, // 'USER', 'FULFILLER'
    participant_id: { type: 'integer', notNull: true },
    status: { type: 'varchar(20)', notNull: true, default: 'OPEN' }, // 'OPEN', 'CLOSED'
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
    last_message_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });
  pgm.createIndex('conversations', ['participant_id', 'status']);

  // 2. Messages Table (Unified for Support and future Order Chat)
  pgm.createTable('messages', {
    id: { type: 'uuid', primaryKey: true, default: pgm.func('gen_random_uuid()') },
    conversation_id: { type: 'uuid', references: '"conversations"', onDelete: 'cascade' },
    order_id: { type: 'integer', references: '"orders"', onDelete: 'cascade' },
    sender_id: { type: 'integer', notNull: true },
    sender_type: { type: 'varchar(20)', notNull: true }, // 'USER', 'FULFILLER', 'ADMIN'
    content: { type: 'text', notNull: true },
    created_at: {
      type: 'timestamp',
      notNull: true,
      default: pgm.func('current_timestamp'),
    },
  });

  // Ensure message belongs to exactly one context
  pgm.addConstraint('messages', 'exactly_one_context', {
    check: '(conversation_id IS NOT NULL AND order_id IS NULL) OR (conversation_id IS NULL AND order_id IS NOT NULL)'
  });

  pgm.createIndex('messages', 'conversation_id');
  pgm.createIndex('messages', 'order_id');
};

exports.down = (pgm) => {
  pgm.dropTable('messages');
  pgm.dropTable('conversations');
};
