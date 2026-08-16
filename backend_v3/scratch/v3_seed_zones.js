const { Pool } = require('pg');
require('dotenv').config({ path: '../.env' });

const seedZones = async () => {
  console.log('🚀 Seeding V3 Zones & Restrictions...');

  const pool = new Pool({
    connectionString: process.env.DATABASE_URL.replace('localhost', '127.0.0.1'),
  });

  try {
    // 1. Create a Restricted Lagos Zone (e.g., Ikeja/Ikoyi)
    // Using a simple square for Lagos Island for demo purposes
    await pool.query(
      `INSERT INTO zones (name, boundary, allowed_fulfiller_classes, flood_prone)
       VALUES ($1, ST_GeogFromText($2), $3, $4)`,
      [
        'Lagos Island (Restricted)',
        'POLYGON((3.39 6.44, 3.45 6.44, 3.45 6.47, 3.39 6.47, 3.39 6.44))',
        ['driver', 'agent'], // RIDER (Motorcycle) EXCLUDED
        true
      ]
    );

    // 2. Create an Open Zone (Mainland)
    await pool.query(
      `INSERT INTO zones (name, boundary, allowed_fulfiller_classes, flood_prone)
       VALUES ($1, ST_GeogFromText($2), $3, $4)`,
      [
        'Lagos Mainland (Standard)',
        'POLYGON((3.30 6.50, 3.40 6.50, 3.40 6.65, 3.30 6.65, 3.30 6.50))',
        ['agent', 'rider', 'driver'],
        false
      ]
    );

    console.log('✅ Zones Seeded.');
    process.exit(0);
  } catch (error) {
    console.error('❌ Seeding Failed:', error.message);
    process.exit(1);
  } finally {
    await pool.end();
  }
};

seedZones();
