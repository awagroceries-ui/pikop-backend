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
            <p>If a delivery fails due to recipient absence, the sender must accept and pay a <strong>50% Return Fee</strong> based on the original fare to have the item returned to the pickup point.</p>
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
            <p>Fulfiller documents are processed via Didit for security and compliance purposes.</p>
        `
    });
};

module.exports = { getTerms, getPrivacyPolicy };
