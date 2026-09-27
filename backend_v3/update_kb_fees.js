const db = require('./src/config/db');

async function updateKbFees() {
    console.log("=== PIKOP KNOWLEDGE BASE FEE UPDATE UTILITY ===");
    try {
        await db.query(`
            UPDATE knowledge_base
            SET content = 'Yes — a 5% platform fee is calculated on the item price (never on the delivery fee) and added to your checkout total whenever you use COD. This fee covers the cost of Pikop holding your payment securely in escrow until your item is confirmed. It''s always shown as its own clear line in your order summary before you pay, never folded invisibly into the item price — we think you should always be able to see exactly what you''re paying for and why.'
            WHERE title ILIKE '%fee for using COD%';

            UPDATE knowledge_base
            SET content = 'The 5% COD fee only applies when you actually choose COD/escrow as your payment method — if you pay upfront normally (card, transfer, USSD) for a non-COD order, there''s no equivalent fee added to your total. Separately, if your order requires SMS to reach someone who isn''t a Pikop app user (for example, sending a delivery to someone without the app), a small ₦50 charge may apply to cover that — this is also always shown clearly before you pay, never hidden.'
            WHERE title ILIKE '%checkout total sometimes include a fee%';

            UPDATE knowledge_base
            SET content = 'You receive 80% of the delivery fee for every completed mission; Pikop retains 20% as its commission. This 80/20 split is applied consistently to every single delivery, whether it''s a standalone Dispatch request or the delivery portion of a Food/Groceries/Shop order — there''s no difference in your share based on order type.'
            WHERE title ILIKE '%much do I actually earn per delivery%';

            UPDATE knowledge_base
            SET content = 'Pikop marketplace commission is 5% across all categories (Food: 5%, Groceries: 5%, Shop: 5%) — calculated on the item price and deducted from your payout on each sale. It''s never added on top of what the customer pays; the customer only sees your listed price plus delivery (and, for COD orders, the separate COD fee, which is paid by them, not you).'
            WHERE title ILIKE '%What commission does Pikop actually take%';

            UPDATE knowledge_base
            SET content = 'Pikop maintains a low 5% commission across all categories (Food, Groceries, Shop) to keep selling viable, competitive, and affordable for all merchants and sellers without squeezing retail margins.'
            WHERE title ILIKE '%Why is the Groceries commission lower%';

            UPDATE knowledge_base
            SET content = 'No — the 5% COD platform fee is paid entirely by the buyer, on top of the item price, at checkout. It is never deducted from what you receive as the seller.'
            WHERE title ILIKE '%accept COD, does the fee cost me anything%';
        `);

        console.log("✅ Knowledge Base fee articles successfully updated to active 5% COD / 20% Dispatch / 5% Marketplace rates!");
    } catch (e) {
        console.error("❌ Knowledge Base update failed:", e.message);
    } finally {
        db.pool.end();
    }
}

updateKbFees();
