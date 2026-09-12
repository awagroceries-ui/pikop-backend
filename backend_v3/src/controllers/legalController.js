/**
 * Serves platform legal documents.
 */
const getTerms = (req, res) => {
    res.render('legal_pages', {
        title: 'Terms & Conditions',
        content: `
            <h4 class="text-primary fw-black mb-4">1. USER AGREEMENT</h4>
            <p>By using Pikop, you agree to comply with our operational guidelines and logistics protocols.</p>

            <h4 class="text-primary fw-black mb-4">2. DELIVERY & REFUND POLICY</h4>
            <p><strong>Strict No-Refund Policy:</strong> In the event that a fulfiller arrives at the designated delivery location but the recipient is absent for collection, the delivery fare is strictly non-refundable.</p>

            <h4 class="text-primary fw-black mb-4">3. RETURN PROTOCOL</h4>
            <p>If a delivery fails due to recipient absence, the sender must accept and pay a <strong>75% Return Fee</strong> based on the original fare to have the item returned to the pickup point.</p>

            <h4 class="text-primary fw-black mb-4">4. CANCELLATION POLICY</h4>
            <p>Cancellations are free while the system is searching for an agent. If a mission is cancelled after an agent is matched but before pickup, a <strong>25% Cancellation Penalty</strong> will be deducted from your wallet. Cancellation is strictly prohibited once the item has been picked up.</p>
        `
    });
};

const getPrivacyPolicy = (req, res) => {
    res.render('legal_pages', {
        title: 'Privacy Policy',
        content: `
            <h4 class="text-primary fw-black mb-4">1. DATA COLLECTION</h4>
            <p>We collect GPS location data to facilitate real-time mission tracking and fulfiller matching.</p>

            <h4 class="text-primary fw-black mb-4">2. IDENTITY VERIFICATION</h4>
            <p>Fulfiller documents are processed via Secure Identity Providers for security and compliance purposes.</p>
        `
    });
};

/**
 * Returns full legal configuration for the mobile app.
 */
const getLegalConfig = (req, res) => {
    res.status(200).json({
        success: true,
        terms_html: `
            <div style="font-family: sans-serif; line-height: 1.6; color: #333;">
                <h2 style="color: #008751;">Terms & Conditions</h2>
                <p><strong>Last Updated: September 2026</strong></p>
                <p>Welcome to Pikop, a logistics and delivery platform operated by Awa Foods & Groceries.</p>

                <h3>1. Operational Protocols</h3>
                <p>Pikop connects Users with independent Fulfillers. Awa Foods is not a common carrier.</p>

                <h3>2. Financial Policies</h3>
                <ul>
                    <li><strong>Agent Share:</strong> 75% of the delivery fare is credited to the Agent.</li>
                    <li><strong>No Refund:</strong> Fees are non-refundable if the recipient is absent at the destination.</li>
                    <li><strong>Return Fee:</strong> Returns are charged at 75% of the original mission fare.</li>
                    <li><strong>Cancellation:</strong> 25% penalty applies if cancelled after an agent is matched. Cancellation is not allowed after pickup.</li>
                </ul>

                <h3>3. Safety & Compliance</h3>
                <p>Fulfillers must undergo mandatory KYC verification. Users must not send illegal or hazardous items.</p>
            </div>
        `,
        privacy_html: `
            <div style="font-family: sans-serif; line-height: 1.6; color: #333;">
                <h2 style="color: #008751;">Privacy Policy</h2>
                <p>We handle your personal and location data in compliance with Nigerian Data Protection Regulations (NDPR).</p>
                <p>GPS data is used strictly for real-time delivery tracking and matching services.</p>
            </div>
        `
    });
};

module.exports = { getTerms, getPrivacyPolicy, getLegalConfig };
