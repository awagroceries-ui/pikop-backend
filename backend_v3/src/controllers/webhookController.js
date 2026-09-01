const db = require('../config/db');
const prembly = require('../services/kyc/PremblyProvider');

/**
 * Authoritative Webhook for Prembly KYC.
 */
const handlePremblyWebhook = async (req, res) => {
    const signature = req.headers['x-identitypass-signature'];
    const payload = req.body;
    const rawBody = req.rawBody;

    // 1. Verify Authenticity (using rawBody to ensure HMAC matches)
    if (!prembly.verifyWebhook(rawBody, signature)) {
        console.error('[Webhook] Prembly: Invalid signature received.');
        // Optionally log rawBody for debugging if trusted
        return res.status(401).send('Unauthorized');
    }

    const { status, customer_reference, verification_type } = payload;
    console.log(`[Webhook] Prembly received: ref=${customer_reference} | type=${verification_type} | status=${status}`);

    try {
        // 2. Identify Fulfiller (customer_reference is the user_ref passed at init)
        const userId = customer_reference.replace('pikop_kyc_', '');

        // 3. Map status to Pikop v3
        const verifiedStatus = (status === 'success' || status === 'verified') ? 'approved' : 'declined';

        // 4. Update Database Idempotently
        await db.query(
            `UPDATE fulfillers
             SET kyc_verification_status = $1,
                 kyc_verified_at = CURRENT_TIMESTAMP,
                 kyc_provider_ref = $2
             WHERE user_id = $3`,
            [verifiedStatus, payload.reference || 'prembly_webhook', userId]
        );

        // 5. Notify Socket (if active)
        try {
            const socketService = require('../services/socketService');
            socketService.getIO().emit('kyc_status_updated', { userId, status: verifiedStatus });
        } catch (e) {}

        res.status(200).send('OK');
    } catch (error) {
        console.error('[Webhook] DB Error:', error.message);
        res.status(500).send('Retry later');
    }
};

/**
 * Simple Redirect handler for Prembly WebView flow.
 */
const handlePremblyRedirect = (req, res) => {
    res.send(`
        <html>
            <body style="font-family: sans-serif; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100vh; text-align: center; padding: 20px;">
                <h2 style="color: #008751;">Verification Step Complete</h2>
                <p>You can now close this window or wait to be returned to the Pikop app automatically.</p>
                <div style="margin-top: 20px; color: #666; font-size: 0.9em;">Status: ${req.query.status || 'Processing'}</div>
            </body>
        </html>
    `);
};

module.exports = {
    handlePremblyWebhook,
    handlePremblyRedirect
};
