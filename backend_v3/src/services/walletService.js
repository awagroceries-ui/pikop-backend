const db = require('../config/db');

/**
 * Ensures a wallet exists for the given owner.
 */
const ensureWalletExists = async (client, ownerType, ownerId) => {
  const { rows } = await client.query(
    "SELECT id FROM wallets WHERE owner_type = $1 AND owner_id = $2",
    [ownerType, ownerId.toString()]
  );

  if (rows.length > 0) return rows[0].id;

  const createRes = await client.query(
    "INSERT INTO wallets (owner_type, owner_id, balance) VALUES ($1, $2, 0) RETURNING id",
    [ownerType, ownerId.toString()]
  );
  return createRes.rows[0].id;
};

/**
 * Records an immutable ledger entry and updates wallet balance.
 */
const recordEntry = async (client, walletId, type, amount, purpose, description, orderId = null) => {
  const numericAmount = parseFloat(amount);

  // 1. Lock and update balance
  const walletRes = await client.query(
    "UPDATE wallets SET balance = balance + $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2 RETURNING balance",
    [type === 'CREDIT' ? numericAmount : -numericAmount, walletId]
  );

  const newBalance = walletRes.rows[0].balance;

  // 2. Record ledger
  await client.query(
    `INSERT INTO wallet_ledger_entries (wallet_id, order_id, entry_type, amount, balance_after, purpose, description)
     VALUES ($1, $2, $3, $4, $5, $6, $7)`,
    [walletId, orderId, type, numericAmount, newBalance, purpose, description]
  );

  return newBalance;
};

/**
 * Processes mission settlement (75/25 Split).
 */
const processMissionSettlement = async (orderId) => {
  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    // 1. Fetch order details
    const orderRes = await client.query(
        "SELECT id, fulfiller_id, total_fare FROM orders WHERE id = $1",
        [orderId]
    );
    const order = orderRes.rows[0];
    if (!order || !order.fulfiller_id) throw new Error('Order not eligible for settlement');

    const totalFare = parseFloat(order.total_fare);

    // 2. Fetch Split Config from Settings (Master Brief v3)
    const settingsRes = await client.query("SELECT value FROM settings WHERE key = 'platform_commission'");
    const commissionRate = parseFloat(settingsRes.rows[0]?.value || '0.25');

    const platformShare = totalFare * commissionRate;
    const fulfillerShare = totalFare - platformShare;

    // 3. Fulfiller Credit
    const fWalletId = await ensureWalletExists(client, 'FULFILLER', order.fulfiller_id);
    await recordEntry(client, fWalletId, 'CREDIT', fulfillerShare, 'SETTLEMENT', `Earnings for Mission #${order.id}`, order.id);

    // 4. Platform Credit
    const pWalletId = await ensureWalletExists(client, 'PLATFORM', 'SYSTEM');
    await recordEntry(client, pWalletId, 'CREDIT', platformShare, 'COMMISSION', `Commission for Mission #${order.id}`, order.id);

    await client.query('COMMIT');
    console.log(`[Wallet] Settled Mission #${order.id}: Fulfiller +${fulfillerShare}, Platform +${platformShare}`);
  } catch (error) {
    await client.query('ROLLBACK');
    console.error('[Wallet] Settlement Failed:', error.message);
    throw error;
  } finally {
    client.release();
  }
};

module.exports = {
  ensureWalletExists,
  recordEntry,
  processMissionSettlement
};
