const { Pool } = require('pg');
const crypto = require('crypto');
require('dotenv').config({ path: '../.env' });

const seedMerchants = async () => {
  console.log('🚀 Seeding V3 Merchant Accounts...');

  const pool = new Pool({
    connectionString: process.env.DATABASE_URL.replace('localhost', '127.0.0.1'),
  });

  try {
    // 1. Generate API Key
    const apiKey = 'pk_live_test_merchant_key_123456';
    const hash = crypto.createHash('sha256').update(apiKey).digest('hex');

    // 2. Create Merchant
    const merchantRes = await pool.query(
      "INSERT INTO merchant_accounts (business_name, contact_email, api_key_hash) VALUES ($1, $2, $3) RETURNING id",
      ['Pikop Logistics Hub', 'merchants@pikop.ng', hash]
    );

    console.log('✅ Merchant Seeded.');
    console.log(`Test API Key: ${apiKey}`);
    process.exit(0);
  } catch (error) {
    console.error('❌ Seeding Failed:', error.message);
    process.exit(1);
  } finally {
    await pool.end();
  }
};

seedMerchants();
