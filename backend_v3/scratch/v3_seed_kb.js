const { Pool } = require('pg');
require('dotenv').config({ path: '../.env' });

const seedKB = async () => {
    console.log('🌱 Seeding V3 Knowledge Base...');

    const pool = new Pool({
        connectionString: process.env.DATABASE_URL.replace('localhost', '127.0.0.1'),
    });

    const articles = [
        { title: 'How to request a delivery?', category: 'General', audience: 'CUSTOMER', content: 'Select pickup/delivery, describe item, get quote, and pay.' },
        { title: 'What items are prohibited?', category: 'Safety', audience: 'BOTH', content: 'No illegal substances, hazardous materials, or high-value currency.' },
        { title: 'How are fares calculated?', category: 'Payments', audience: 'BOTH', content: 'Fares = Base Fee (Size Tier) + Distance (NGN 150/KM).' },
        { title: 'How to start earning?', category: 'General', audience: 'FULFILLER', content: 'Complete KYC, go online, and wait for mission alerts.' }
    ];

    try {
        for (const art of articles) {
            await pool.query(
                "INSERT INTO knowledge_base (title, category, target_audience, content) VALUES ($1, $2, $3, $4)",
                [art.title, art.category, art.audience, art.content]
            );
        }
        console.log('✅ Knowledge Base Seeded.');
        process.exit(0);
    } catch (error) {
        console.error('❌ Seeding Failed:', error.message);
        process.exit(1);
    } finally {
        await pool.end();
    }
};

seedKB();
