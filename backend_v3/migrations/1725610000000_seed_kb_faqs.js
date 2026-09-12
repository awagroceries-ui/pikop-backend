exports.up = (pgm) => {
  // Typical FAQs for Customers
  pgm.sql(`
    INSERT INTO knowledge_base (title, content, category, target_audience, priority) VALUES
    ('How do I place a delivery request?', 'Open the app, select "New Delivery", enter pickup and delivery locations, describe the item, and pay securely via Paystack.', 'App Navigation', 'CUSTOMER', 10),
    ('What are the cancellation fees?', 'Cancellations are free while we search for an agent. Once an agent is matched, a 25% penalty fee applies. No cancellations are allowed after pickup.', 'Policies', 'CUSTOMER', 9),
    ('What happens if the recipient is absent?', 'If our agent arrives and the recipient is unavailable, the mission is marked as failed. Per policy, the fare is non-refundable.', 'Policies', 'CUSTOMER', 8),
    ('How do I initiate a return?', 'If a delivery fails, you can initiate a return from the mission details. Returns are charged at 75% of the original mission fare.', 'Policies', 'CUSTOMER', 7),
    ('What items are prohibited?', 'Illegal drugs, weapons, explosives, hazardous chemicals, and large amounts of cash are strictly banned. Discovery will result in police reporting and disposal of the item.', 'Policies', 'CUSTOMER', 6),
    ('Can I pay for the item via Pikop?', 'Yes! Use "Secure Pay" (Escrow). You pay upfront, we hold the funds, and release them to the seller only when you confirm receipt.', 'Wallet & Payments', 'CUSTOMER', 5);
  `);

  // Typical FAQs for Agents (Fulfillers)
  pgm.sql(`
    INSERT INTO knowledge_base (title, content, category, target_audience, priority) VALUES
    ('How do I start receiving missions?', 'Toggle the "Go Online" switch on your dashboard. Ensure your GPS is on and you are within an active operational zone.', 'App Navigation', 'FULFILLER', 10),
    ('How much do I earn per mission?', 'Fulfillers receive 75% of the delivery fare. The remaining 25% is the platform commission for Awa Foods & Groceries.', 'Earnings', 'FULFILLER', 9),
    ('When can I withdraw my earnings?', 'Once a mission is completed, your share is instantly credited to your wallet. You can request a withdrawal to your bank account anytime.', 'Withdrawals', 'FULFILLER', 8),
    ('How do I verify a pickup?', 'Ask the sender for the 4-digit "Pickup Code" shown on their screen. Enter it in your app to officially start the mission.', 'App Navigation', 'FULFILLER', 7),
    ('What is the 10-minute wait rule?', 'Upon arrival at the destination, if the recipient is not reachable, you must wait at least 10 minutes before marking the mission as "Failed".', 'Policies', 'FULFILLER', 6);
  `);

  // General FAQs
  pgm.sql(`
    INSERT INTO knowledge_base (title, content, category, target_audience, priority) VALUES
    ('How do I contact support?', 'You can start a live chat with our support team directly from the "Support & Help Center" menu in the app.', 'General', 'BOTH', 5);
  `);
};

exports.down = (pgm) => {
  pgm.sql('DELETE FROM knowledge_base WHERE category IN (\'App Navigation\', \'Policies\', \'Earnings\', \'Withdrawals\', \'Wallet & Payments\', \'General\')');
};
