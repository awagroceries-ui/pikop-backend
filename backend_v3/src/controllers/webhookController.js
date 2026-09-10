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
        // 2. Identify Fulfiller (customer_reference is 'pikop_kyc_{userId}')
        const userIdRaw = customer_reference.replace('pikop_kyc_', '');
        const userId = parseInt(userIdRaw);

        if (isNaN(userId)) {
            console.error(`[Webhook] Invalid userId derived from reference: ${customer_reference}`);
            return res.status(400).send('Invalid Reference');
        }

        // 3. Map status to Pikop v3
        const verifiedStatus = (status === 'success' || status === 'verified') ? 'approved' : 'declined';

        // 4. Update Database Idempotently
        // We also update kyc_status to move it forward if approved.
        const updateRes = await db.query(
            `UPDATE fulfillers
             SET didit_verification_status = $1,
                 kyc_status = CASE WHEN $1 = 'approved' THEN 'PENDING_REVIEW' ELSE kyc_status END,
                 kyc_verified_at = CURRENT_TIMESTAMP,
                 kyc_provider_ref = $2,
                 kyc_details = $3
             WHERE user_id = $4
             RETURNING id, kyc_status`,
            [verifiedStatus, payload.reference || 'prembly_webhook', JSON.stringify(payload), userId]
        );

        if (updateRes.rows.length === 0) {
            console.warn(`[Webhook] No fulfiller profile found for user_id ${userId}. Webhook processed but not saved.`);
        } else {
            console.log(`[Webhook] User ${userId} (${updateRes.rows[0].id}) updated to ${verifiedStatus}. Main Status: ${updateRes.rows[0].kyc_status}`);
        }

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

/**
 * Webhook for Termii SMS Delivery Reports.
 */
const handleTermiiWebhook = async (req, res) => {
    // Authorized token for Termii (Signing Secret provided)
    const secretToken = process.env.TERMII_WEBHOOK_TOKEN || 'tsk_aMngGOk22bKBmOATkpceSlKtoG';
    const inboundToken = req.query.token || req.headers['x-termii-token'];

    if (inboundToken !== secretToken) {
        console.warn('[Webhook] Termii: Unauthorized attempt with token:', inboundToken);
        return res.status(401).send('Unauthorized');
    }

    const payload = req.body;
    // Termii typically sends: { message_id: "...", status: "Delivered", recipient: "...", ... }

    const { message_id, status, recipient } = payload;
    console.log(`[Webhook] Termii delivery report: ref=${message_id} | status=${status} | to=${recipient}`);

    try {
        await db.query(
            "UPDATE sms_logs SET status = $1 WHERE provider_ref = $2",
            [status ? status.toLowerCase() : 'unknown', message_id]
        );
        res.status(200).send('OK');
    } catch (error) {
        console.error('[Webhook] Termii DB Error:', error.message);
        res.status(500).send('Retry later');
    }
};

module.exports = {
    handlePremblyWebhook,
    handlePremblyRedirect,
    handleTermiiWebhook
};
