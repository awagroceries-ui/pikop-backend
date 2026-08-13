const db = require('../config/db');
const emailService = require('./emailService');

/**
 * Generates and emails a monthly summary to a corporate account.
 */
const generateMonthlyInvoice = async (corporateAccountId, month, year) => {
    try {
        const { rows: account } = await db.query("SELECT company_name, billing_email FROM corporate_accounts WHERE id = $1", [corporateAccountId]);
        if (account.length === 0) return;

        const { rows: orders } = await db.query(`
            SELECT id, total_fare, pickup_address, delivery_address, created_at
            FROM orders
            WHERE corporate_account_id = $1
            AND EXTRACT(MONTH FROM created_at) = $2
            AND EXTRACT(YEAR FROM created_at) = $3
            ORDER BY created_at ASC
        `, [corporateAccountId, month, year]);

        if (orders.length === 0) return;

        const totalSpent = orders.reduce((sum, o) => sum + parseFloat(o.total_fare), 0);

        let html = `<h1>Monthly Activity Summary - ${account[0].company_name}</h1>`;
        html += `<p>Period: ${month}/${year}</p>`;
        html += `<table border="1" cellpadding="5" style="border-collapse: collapse;">
                    <thead><tr><th>Order ID</th><th>Date</th><th>Route</th><th>Fare</th></tr></thead>
                    <tbody>`;

        orders.forEach(o => {
            html += `<tr>
                        <td>${o.id.toString().substring(0, 8)}</td>
                        <td>${new Date(o.created_at).toLocaleDateString()}</td>
                        <td>${o.pickup_address} -> ${o.delivery_address}</td>
                        <td>₦${Number(o.total_fare).toLocaleString()}</td>
                    </tr>`;
        });

        html += `</tbody></table>`;
        html += `<h3>Total Volume: ₦${totalSpent.toLocaleString()}</h3>`;
        html += `<p>Note: This is a record-keeping summary. All orders were paid in full at the time of creation.</p>`;

        await emailService.sendMail(
            account[0].billing_email,
            `[Pikop] Monthly Activity Report - ${month}/${year}`,
            html
        );

        console.log(`[Corporate] Sent invoice for ${account[0].company_name}`);
    } catch (error) {
        console.error(`[Corporate] Invoice generation failed for ${corporateAccountId}:`, error.message);
    }
};

module.exports = {
    generateMonthlyInvoice
};
