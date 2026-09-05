exports.up = (pgm) => {
  // 1. Add paystack_recipient_code to fulfillers for faster future transfers
  pgm.addColumns('fulfillers', {
    paystack_recipient_code: { type: 'varchar(100)' }
  });

  // 2. Add bank details if not already present (v3 baseline might have missed them)
  // These are required to create a Transfer Recipient on Paystack
  pgm.addColumns('fulfillers', {
    bank_name: { type: 'varchar(100)' },
    account_number: { type: 'varchar(20)' },
    bank_code: { type: 'varchar(10)' }
  });

  // 3. Ensure withdrawals status check is robust
  pgm.sql(`
    ALTER TABLE "withdrawals" DROP CONSTRAINT IF EXISTS "withdrawals_status_check";
    ALTER TABLE "withdrawals" ADD CONSTRAINT "withdrawals_status_check"
    CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCESSFUL', 'FAILED', 'REVERSED'));
  `);
};

exports.down = (pgm) => {
  pgm.removeColumns('fulfillers', ['paystack_recipient_code', 'bank_name', 'account_number', 'bank_code']);
};
