const db = require('../config/db');
const path = require('path');
const fs = require('fs');

/**
 * Monthly Invoice Generation Job
 * Summarizes the prior month's orders for every active corporate account.
 */
const generateMonthlyInvoices = async () => {
    console.log('🚀 Starting Monthly Invoice Generation Job...');

    try {
        // 1. Get all active corporate accounts
        const { rows: accounts } = await db.query("SELECT * FROM corporate_accounts WHERE status = 'active'");

        for (const acc of accounts) {
            console.log(`- Processing Account: ${acc.company_name} (${acc.id})`);

            // 2. Fetch last month's orders
            const { rows: orders } = await db.query(`
                SELECT id, total_fare, created_at
                FROM orders
                WHERE corporate_account_id = $1
                AND created_at >= date_trunc('month', CURRENT_DATE - INTERVAL '1 month')
                AND created_at < date_trunc('month', CURRENT_DATE)
                ORDER BY created_at ASC`, [acc.id]);

            if (orders.length === 0) {
                console.log(`  ℹ️ No orders for ${acc.company_name} last month. Skipping.`);
                continue;
            }

            // 3. Generate Stub PDF Record (Simulated)
            const reportPath = path.join(__dirname, `../../uploads/invoices/invoice-${acc.id}-${Date.now()}.txt`);
            const total = orders.reduce((sum, o) => sum + parseFloat(o.total_fare), 0);

            let content = `INVOICE FOR: ${acc.company_name}\n`;
            content += `Period: ${new Date(Date.now() - 30 * 24 * 3600 * 1000).toLocaleDateString()} to Now\n\n`;
            orders.forEach(o => {
                content += `ORDER #${o.id} | Fare: ₦${o.total_fare} | Date: ${o.created_at}\n`;
            });
            content += `\nTOTAL FOR PERIOD: ₦${total.toFixed(2)}`;

            if (!fs.existsSync(path.join(__dirname, '../../uploads/invoices'))) {
                fs.mkdirSync(path.join(__dirname, '../../uploads/invoices'), { recursive: true });
            }
            fs.writeFileSync(reportPath, content);

            console.log(`  ✅ Invoice generated: ${reportPath}`);
            // TODO: Email this content to acc.billing_email
        }

        console.log('✅ Job completed.');
        process.exit(0);
    } catch (error) {
        console.error('❌ Invoice Job Failed:', error.message);
        process.exit(1);
    }
};

generateMonthlyInvoices();
