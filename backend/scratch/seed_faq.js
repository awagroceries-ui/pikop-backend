const { Pool } = require('pg');
require('dotenv').config({ path: '../.env' });

const seedFAQ = async () => {
    console.log('🌱 Seeding Knowledge Base (FAQ)...');

    const pool = new Pool({
        connectionString: process.env.DATABASE_URL.replace('localhost', '127.0.0.1'),
    });

    const articles = [
        // CUSTOMER FAQs
        {
            title: 'How do I request a delivery?',
            category: 'General',
            audience: 'CUSTOMER',
            content: '1. Open the Pikop app.\n2. Enter your pickup and delivery locations.\n3. Add a photo and description of the item.\n4. Review the fare quote and confirm.'
        },
        {
            title: 'What items can I send?',
            category: 'Safety',
            audience: 'CUSTOMER',
            content: 'You can send documents, parcels, groceries, and small electronics. We prohibit illegal substances, hazardous materials, and items weighing more than 50kg for standard missions.'
        },
        {
            title: 'How do I pay for my mission?',
            category: 'Payments',
            audience: 'CUSTOMER',
            content: 'We support multiple payment options via Paystack, including Card, Bank Transfer, USSD, and Mobile Money.'
        },
        // FULFILLER FAQs
        {
            title: 'How do I start receiving offers?',
            category: 'Missions',
            audience: 'FULFILLER',
            content: 'Once your KYC is approved, simply toggle the "Online" switch on your dashboard to start receiving nearby delivery requests.'
        },
        {
            title: 'How do my earnings work?',
            category: 'Payments',
            audience: 'FULFILLER',
            content: 'You keep 75% of every delivery fare. Earnings are credited to your wallet immediately after a successful mission delivery.'
        },
        {
            title: 'What documents are required for KYC?',
            category: 'Account',
            audience: 'FULFILLER',
            content: 'Agents require a valid ID. Riders and Drivers require a valid Driver\'s License and vehicle registration details.'
        }
    ];

    try {
        for (const art of articles) {
            await pool.query(
                "INSERT INTO knowledge_base (title, category, target_audience, content, is_active) VALUES ($1, $2, $3, $4, true)",
                [art.title, art.category, art.audience, art.content]
            );
        }
        console.log('✅ Knowledge Base seeded successfully.');
        process.exit(0);
    } catch (error) {
        console.error('❌ Seeding Failed:', error.message);
        process.exit(1);
    } finally {
        await pool.end();
    }
};

seedFAQ();
