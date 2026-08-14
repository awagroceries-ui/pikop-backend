const app = require('./app');
const http = require('http');
const socketService = require('./services/socketService');
const cron = require('node-cron');
const corporateService = require('./services/corporateService');
const db = require('./config/db');
require('dotenv').config();

const PORT = process.env.PORT || 3000;
const server = http.createServer(app);

// Initialize Socket.io
socketService.init(server);

// Scheduled Monthly Invoices (Prompt 1)
// Runs at 00:01 on the 1st of every month
cron.schedule('1 0 1 * *', async () => {
    console.log('[Cron] Starting monthly corporate invoice generation...');
    try {
        const { rows: accounts } = await db.query("SELECT id FROM corporate_accounts WHERE status = 'active'");
        const lastMonth = new Date();
        lastMonth.setMonth(lastMonth.getMonth() - 1);
        const month = lastMonth.getMonth() + 1;
        const year = lastMonth.getFullYear();

        for (const acc of accounts) {
            await corporateService.generateMonthlyInvoice(acc.id, month, year);
        }
    } catch (e) {
        console.error('[Cron] Invoice job failed:', e.message);
    }
});

server.listen(PORT, () => {
  console.log(`Pikop API Server running on port ${PORT}`);
});
