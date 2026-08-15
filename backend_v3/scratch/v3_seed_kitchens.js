const { Pool } = require('pg');
require('dotenv').config({ path: '../.env' });

const seedKitchens = async () => {
  console.log('🌱 Seeding V3 Foods & Meals...');

  const pool = new Pool({
    connectionString: process.env.DATABASE_URL.replace('localhost', '127.0.0.1'),
  });

  try {
    // 1. Create a dummy kitchen
    const kitchenRes = await pool.query(
      `INSERT INTO kitchens (business_name, cac_number, contact_email, status, city, cuisine_type)
       VALUES ($1, $2, $3, $4, $5, $6) RETURNING id`,
      ['Lagos Mama Kitchen', 'CAC998877', 'mama@lagos.com', 'active', 'Lagos', 'Local Nigerian']
    );
    const kitchenId = kitchenRes.rows[0].id;

    // 2. Add menu items
    const items = [
      { name: 'Jollof Rice Special', price: 2500, category: 'Main' },
      { name: 'Pounded Yam & Egusi', price: 3500, category: 'Main' },
      { name: 'Fresh Palm Wine', price: 1200, category: 'Drinks' }
    ];

    for (const item of items) {
      await pool.query(
        "INSERT INTO menu_items (kitchen_id, name, price, category, available) VALUES ($1, $2, $3, $4, true)",
        [kitchenId, item.name, item.price, item.category]
      );
    }

    console.log('✅ Foods Module Seeded.');
    process.exit(0);
  } catch (error) {
    console.error('❌ Seeding Failed:', error.message);
    process.exit(1);
  } finally {
    await pool.end();
  }
};

seedKitchens();
