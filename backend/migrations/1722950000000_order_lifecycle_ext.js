exports.up = (pgm) => {
  // 1. Extend Orders Table
  pgm.addColumns('orders', {
    item_photo_url: { type: 'text' },
    delivery_photo_url: { type: 'text' },
    pickup_display_summary: { type: 'varchar(255)' },
    delivery_display_summary: { type: 'varchar(255)' },
    cancellation_fee_waived: { type: 'boolean', notNull: true, default: false },
    incident_dispute_id: { type: 'integer', references: '"disputes"', onDelete: 'set null' }
  });

  // 2. Add Category and Resolution to Disputes Table
  pgm.addColumns('disputes', {
    category: {
      type: 'varchar(50)',
      notNull: true,
      default: 'other',
      // lost_item, damaged_item, fare_dispute, conduct, no_show, other, incident_breakdown, incident_accident, incident_security_risk, incident_other
    },
    resolution: {
      type: 'varchar(50)',
      // refund, partial_refund, fulfiller_strike, no_action, fee_waived
    }
  });

  // 3. Update existing records pickup/delivery summaries based on addresses (approximate)
  pgm.sql("UPDATE orders SET pickup_display_summary = substring(pickup_address from 1 for 50), delivery_display_summary = substring(delivery_address from 1 for 50)");
};

exports.down = (pgm) => {
  pgm.removeColumns('orders', [
    'item_photo_url',
    'delivery_photo_url',
    'pickup_display_summary',
    'delivery_display_summary',
    'cancellation_fee_waived',
    'incident_dispute_id'
  ]);
  pgm.removeColumns('disputes', ['category', 'resolution']);
};
