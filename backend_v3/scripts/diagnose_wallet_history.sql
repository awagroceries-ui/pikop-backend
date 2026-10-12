-- Pikop diagnostic: issues #10 (empty mission history) / #11 (wallet stuck at zero)
--
-- I built a clean copy of this backend against a fresh Postgres database,
-- ran migrations, and drove the full accept -> pickup -> deliver flow through
-- the real HTTP endpoints for a VERIFIED, ONLINE agent. Every step worked
-- correctly: fulfiller_id was set on accept, the delivery settlement credited
-- the agent's wallet (80/20 split), GET /api/v1/fulfillers/orders returned
-- the mission, and GET /api/v1/wallets/me returned the correct balance.
--
-- So the settlement/history code itself is not broken for a normally
-- provisioned agent. That means your production symptom is almost certainly
-- a DATA problem on your live database (or your live server is running older
-- code than this fix pass) rather than a logic bug. Run the queries below
-- against your REAL production database (read-only, nothing here writes
-- anything) and send me the output -- each one targets a specific known way
-- this can silently break.

-- 1. Fulfillers whose account is VERIFIED/ONLINE but has no linked user_id.
--    acceptOrder/getFulfillerOrders/wallet all resolve through fulfillers.user_id;
--    if this is NULL (or points to a deleted user), every one of those calls
--    silently returns empty/zero for that agent no matter what else is correct.
SELECT id, full_name, email, phone, primary_class, kyc_status, online_status, user_id
FROM fulfillers
WHERE user_id IS NULL
   OR user_id NOT IN (SELECT id FROM users);

-- 2. Fulfillers whose email doesn't match any user account at all (breaks the
--    auto-heal-by-email fallback that resolveFulfiller() relies on when #1 happens).
SELECT f.id, f.full_name, f.email AS fulfiller_email, u.id AS matching_user_id
FROM fulfillers f
LEFT JOIN users u ON u.email ILIKE f.email
WHERE f.user_id IS NULL AND u.id IS NULL;

-- 3. Orders with a fulfiller_id that doesn't point to a real fulfillers row
--    (shouldn't be possible given the FK, but confirms the FK is actually live).
SELECT o.id, o.status, o.fulfiller_id
FROM orders o
LEFT JOIN fulfillers f ON f.id = o.fulfiller_id
WHERE o.fulfiller_id IS NOT NULL AND f.id IS NULL;

-- 4. Delivered orders that were NEVER settled (no SETTLEMENT ledger entry).
--    If this returns rows, settlement is genuinely failing for real missions --
--    check your server logs around "[Wallet] Settlement Failed" /
--    "[VerifyDelivery] Wallet Settlement Warning" for the actual error.
SELECT o.id, o.status, o.fulfiller_id, o.delivered_at
FROM orders o
WHERE o.status IN ('DELIVERED', 'RELEASED')
  AND o.fulfiller_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM wallet_ledger_entries wle
      WHERE wle.order_id = o.id AND wle.purpose = 'SETTLEMENT'
  );

-- 5. Per-agent reconciliation: what SHOULD be in their wallet (sum of
--    SETTLEMENT/bonus credits minus debits for their linked USER wallet)
--    vs what the wallets table actually shows. A mismatch here means the
--    wallet row itself is out of sync with its own ledger (shouldn't happen
--    given recordEntry() updates both atomically, but worth ruling out).
SELECT
    f.id AS fulfiller_id,
    f.full_name,
    f.user_id,
    w.id AS wallet_id,
    w.balance AS wallet_table_balance,
    COALESCE(SUM(CASE WHEN wle.entry_type = 'CREDIT' THEN wle.amount ELSE -wle.amount END), 0) AS ledger_computed_balance
FROM fulfillers f
LEFT JOIN wallets w ON w.owner_type = 'USER' AND w.owner_id = f.user_id::text
LEFT JOIN wallet_ledger_entries wle ON wle.wallet_id = w.id
WHERE f.kyc_status = 'VERIFIED'
GROUP BY f.id, f.full_name, f.user_id, w.id, w.balance
HAVING w.id IS NULL OR w.balance <> COALESCE(SUM(CASE WHEN wle.entry_type = 'CREDIT' THEN wle.amount ELSE -wle.amount END), 0);

-- 6. Orders stuck with queued_for_fulfiller_id set but never promoted
--    (would explain "queued missions never show" specifically).
SELECT id, status, fulfiller_id, queued_for_fulfiller_id, created_at
FROM orders
WHERE queued_for_fulfiller_id IS NOT NULL
  AND status = 'QUEUED'
ORDER BY created_at ASC;
