const db = require('../config/db');

/**
 * Gets the authenticated user's wallet information and recent transactions.
 */
const getWalletInfo = async (req, res) => {
  const userId = req.user.id;

  try {
    // 1. Check if user is a fulfiller (to determine ownerType)
    const fulfillerRes = await db.query("SELECT id FROM fulfillers WHERE user_id = $1", [userId]);
    const isFulfiller = fulfillerRes.rows.length > 0;
    const ownerType = isFulfiller ? 'FULFILLER' : 'USER';

    // 2. Get Wallet Balance
    const walletRes = await db.query(
      "SELECT id, balance, currency FROM wallets WHERE owner_id = $1 AND owner_type = $2",
      [userId, ownerType]
    );

    if (walletRes.rows.length === 0) {
      return res.status(200).json({ balance: 0, currency: 'NGN', transactions: [] });
    }

    const wallet = walletRes.rows[0];

    // 2. Get Recent Transactions (Ledger Entries)
    const ledgerRes = await db.query(
      `SELECT id, amount, entry_type, purpose, reference_id, created_at
       FROM wallet_ledger_entries
       WHERE wallet_id = $1
       ORDER BY created_at DESC
       LIMIT 20`,
      [wallet.id]
    );

    res.status(200).json({
      balance: parseFloat(wallet.balance),
      currency: wallet.currency,
      transactions: ledgerRes.rows
    });
  } catch (error) {
    console.error('Get Wallet Info Error:', error);
    res.status(500).json({ error: 'Failed to fetch wallet information' });
  }
};

module.exports = {
  getWalletInfo
};
