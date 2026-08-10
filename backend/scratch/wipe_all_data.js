const { Pool } = require('pg');
const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '../.env') });

const wipeData = async () => {
    console.log('🗑️ Starting Full Platform Data Wipe (Cleanup)...');

    // Handle localhost/@ username issues
    let connString = process.env.DATABASE_URL;
    if (connString && connString.includes('localhost')) {
        connString = connString.replace('localhost', '127.0.0.1');
    }

    const pool = new Pool({
        connectionString: connString,
    });

    try {
        await pool.query('BEGIN');

        console.log('- Wiping Transactions, History & Sessions...');
        await pool.query('TRUNCATE wallet_ledger_entries, withdrawals, order_status_history, disputes, saved_addresses, promo_code_redemptions, referral_rewards, user_sessions, otp_verifications CASCADE');

        console.log('- Wiping Missions, Quotes & Messages...');
        await pool.query('TRUNCATE orders, quotes, messages, conversations CASCADE');

        console.log('- Wiping Fulfillers & Vehicles...');
        await pool.query('TRUNCATE fulfillers, vehicles, kyc_documents CASCADE');

        console.log('- Wiping Users & Corporate Accounts...');
        await pool.query('TRUNCATE users, corporate_accounts, corporate_sub_accounts CASCADE');

        await pool.query('COMMIT');
        console.log('✅ All test signup data and mission records wiped successfully.');
        console.log('System is now in a clean state. Please create your admin account again.');
        process.exit(0);
    } catch (error) {
        await pool.query('ROLLBACK');
        console.error('❌ Wipe Failed:', error.message);
        process.exit(1);
    } finally {
        await pool.end();
    }
};

wipeData();
