/**
 * Serves platform legal documents.
 */
const getTerms = (req, res) => {
    res.render('legal_pages', {
        title: 'Terms & Conditions',
        content: `
            <div class="legal-content">
                <h4 class="text-primary fw-black mb-4">1. USER AGREEMENT & ELIGIBILITY</h4>
                <p>By using the Pikop platform, you agree to these Terms and Conditions. You must be at least 18 years of age and possess the legal capacity to enter into binding contracts.</p>

                <h4 class="text-primary fw-black mb-4">2. PROHIBITED ITEMS, REPORTING & DISPOSAL</h4>
                <p>The transportation of illegal drugs, weapons, explosives, hazardous chemicals, live animals, or large quantities of cash is strictly prohibited. <strong>Discovery of any prohibited items will result in the immediate reporting of the Sender and the item to the appropriate law enforcement authorities. Furthermore, such items will be discarded or surrendered to the authorities accordingly without any liability or compensation to the sender.</strong></p>

                <h4 class="text-primary fw-black mb-4">3. INDEMNIFICATION (HOLD HARMLESS)</h4>
                <p>You agree to indemnify, defend, and hold harmless Awa Foods & Groceries (Pikop), its parent company, subsidiaries, affiliates, and independent Fulfillers from and against any and all claims, losses, expenses, or demands of liability, including legal fees, arising from your use of the service or the discovery/disposal of prohibited items.</p>

                <h4 class="text-primary fw-black mb-4">4. DELIVERY, CANCELLATION & RETURN POLICY</h4>
                <ul>
                    <li><strong>No Refund:</strong> Fees are non-refundable if the recipient is absent upon the agent's arrival. Agents must wait a mandatory 10 minutes before marking a mission as failed.</li>
                    <li><strong>Cancellation:</strong> A 25% penalty fee applies if cancelled after an agent is matched but before pickup. Cancellation is strictly prohibited after pickup.</li>
                    <li><strong>Returns:</strong> Failed deliveries requiring return to the sender are charged at 75% of the original fare.</li>
                </ul>

                <h4 class="text-primary fw-black mb-4">5. GOVERNING LAW & JURISDICTION</h4>
                <p>These terms are governed by the laws of the Federal Republic of Nigeria. Any legal proceedings shall be directed to the courts of Port Harcourt, Rivers State.</p>
            </div>
        `
    });
};

const getPrivacyPolicy = (req, res) => {
    res.render('legal_pages', {
        title: 'Privacy Policy',
        content: `
            <div class="legal-content">
                <h4 class="text-primary fw-black mb-4">1. DATA COLLECTION</h4>
                <p>We collect personal information including names, contact details, and precise GPS location data. GPS tracking is essential for matching you with nearby agents and providing real-time mission status.</p>

                <h4 class="text-primary fw-black mb-4">2. INFORMATION SHARING</h4>
                <p>Your name and location are shared with the assigned Fulfiller to facilitate delivery. We do not sell your personal data to third parties. All financial data is processed securely via Paystack.</p>

                <h4 class="text-primary fw-black mb-4">3. COMPLIANCE (NDPR)</h4>
                <p>We handle your data in accordance with the Nigeria Data Protection Regulation (NDPR). You have the right to request access to or deletion of your personal records at any time.</p>
            </div>
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
            <div style="font-family: sans-serif; line-height: 1.6; color: #333; padding: 10px;">
                <h2 style="color: #008751;">Terms & Conditions</h2>
                <p><strong>Last Updated: September 2026</strong></p>
                <p>Pikop is a logistics engine operated by Awa Foods & Groceries.</p>

                <h3 style="color: #008751;">1. Prohibited Items & Disposal</h3>
                <p>Sending illegal goods, drugs, or weapons is strictly banned. <strong>Discoveries will be reported to the police along with sender details. Prohibited items will be discarded immediately without refund.</strong></p>

                <h3 style="color: #008751;">2. Hold Harmless Clause</h3>
                <p>Users agree to hold Awa Foods & Groceries and its independent agents harmless against any legal claims or losses arising from their use of the platform.</p>

                <h3 style="color: #008751;">3. Financial Policies</h3>
                <ul style="padding-left: 20px;">
                    <li><strong>25% Cancellation Fee:</strong> Applied if cancelled after an agent is matched (pre-pickup).</li>
                    <li><strong>No Cancellation:</strong> Allowed after an item has been picked up.</li>
                    <li><strong>75% Return Fee:</strong> Applied for failed deliveries needing return.</li>
                    <li><strong>Recipient Absent:</strong> Non-refundable if agent waits 10 minutes at destination.</li>
                </ul>

                <h3 style="color: #008751;">4. Jurisdiction</h3>
                <p>Governed by Nigerian Law. Jurisdiction: Port Harcourt, Rivers State.</p>
            </div>
        `,
        privacy_html: `
            <div style="font-family: sans-serif; line-height: 1.6; color: #333; padding: 10px;">
                <h2 style="color: #008751;">Privacy Policy</h2>
                <p>We process GPS data and KYC documents to ensure platform security and operational accuracy.</p>
                <p>We comply with NDPR (Nigeria Data Protection Regulation) guidelines.</p>
            </div>
        `
    });
};

module.exports = { getTerms, getPrivacyPolicy, getLegalConfig };
