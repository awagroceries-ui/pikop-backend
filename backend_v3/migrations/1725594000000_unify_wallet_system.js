exports.up = async (pgm) => {
  // 1. Data Integrity Heal: Merge FULFILLER balances into corresponding USER wallets
  // This recovers all "missing" money sitting in the redundant pockets.
  pgm.sql(`
    DO $$
    DECLARE
        source_wallet RECORD;
        target_user_id INTEGER;
        target_wallet_id UUID;
    BEGIN
        -- 1. Loop through all wallets owned by FULFILLERS
        FOR source_wallet IN SELECT * FROM wallets WHERE owner_type = 'FULFILLER' LOOP

            -- Find the user_id linked to this fulfiller
            SELECT user_id INTO target_user_id FROM fulfillers WHERE id = source_wallet.owner_id::integer;

            IF target_user_id IS NOT NULL THEN
                -- Find or create the target USER wallet for this person
                INSERT INTO wallets (owner_type, owner_id, balance, pending_balance)
                VALUES ('USER', target_user_id::text, 0, 0)
                ON CONFLICT (owner_type, owner_id) DO NOTHING;

                SELECT id INTO target_wallet_id FROM wallets WHERE owner_type = 'USER' AND owner_id = target_user_id::text;

                -- Move ledger history to the unified wallet
                UPDATE wallet_ledger_entries
                SET wallet_id = target_wallet_id
                WHERE wallet_id = source_wallet.id;

                -- Merge balances (Available and Pending)
                UPDATE wallets
                SET
                    balance = balance + source_wallet.balance,
                    pending_balance = pending_balance + source_wallet.pending_balance,
                    updated_at = NOW()
                WHERE id = target_wallet_id;

                -- Move withdrawals to the new wallet reference
                UPDATE withdrawals
                SET wallet_id = target_wallet_id
                WHERE wallet_id = source_wallet.id;

                -- Log the merge
                RAISE NOTICE 'Merged Wallet % (Fulfiller %) into Wallet % (User %)', source_wallet.id, source_wallet.owner_id, target_wallet_id, target_user_id;
            END IF;
        END LOOP;
    END $$;
  `);

  // 2. Remove the FULFILLER wallets now that they are empty and history is moved
  pgm.sql("DELETE FROM wallets WHERE owner_type = 'FULFILLER';");
};

exports.down = (pgm) => {
  // Down migration for wallet unification is high-risk as it would require splitting balances back.
  // We leave it as a no-op to protect data integrity.
};
