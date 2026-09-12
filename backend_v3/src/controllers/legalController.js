/**
 * Serves standardized platform legal documents.
 */
const getTerms = (req, res) => {
    res.render('legal_pages', {
        title: 'Terms & Conditions',
        content: `
            <div class="legal-standard">
                <h4 class="text-primary fw-black mb-3">1. DEFINITIONS</h4>
                <p><strong>Platform:</strong> Pikop, operated by Awa Foods & Groceries.<br>
                <strong>User:</strong> Any individual or entity utilizing the Platform for logistics services.<br>
                <strong>Fulfiller:</strong> Independent service providers (couriers/drivers) who fulfill mission requests.</p>

                <h4 class="text-primary fw-black mb-3">2. THE PIKOP SERVICE</h4>
                <p>Pikop acts as a technology intermediary and <strong>Commercial Agent</strong> for independent Fulfillers. Awa Foods does not provide transportation services directly. When a User pays Pikop, the legal obligation to pay the Fulfiller is considered fulfilled.</p>

                <h4 class="text-primary fw-black mb-3">3. PROHIBITED ITEMS & ENFORCEMENT</h4>
                <p>Transportation of illegal drugs, weapons, explosives, or hazardous materials is strictly prohibited. <strong>Discovery of prohibited items will result in the immediate reporting of the Sender and the item to the appropriate authorities. Furthermore, such items will be discarded or surrendered to authorities immediately without any liability or compensation to the sender.</strong></p>

                <h4 class="text-primary fw-black mb-3">4. FINANCIAL POLICIES</h4>
                <ul>
                    <li><strong>Earnings Split:</strong> Fulfillers receive 75% of the delivery fare.</li>
                    <li><strong>Cancellation:</strong> A 25% penalty applies if cancelled after an agent is matched but before pickup. Cancellation is prohibited after pickup.</li>
                    <li><strong>Absence:</strong> Fares are non-refundable if the recipient is absent after a mandatory 10-minute agent wait.</li>
                    <li><strong>Returns:</strong> Charged at 75% of the original mission fare.</li>
                    <li><strong>Refunds:</strong> All approved refunds are issued exclusively as <strong>Pikop Wallet Credits</strong>.</li>
                </ul>

                <h4 class="text-primary fw-black mb-3">5. LIABILITY & INDEMNIFICATION</h4>
                <p><strong>Limitation of Liability:</strong> Pikop’s total liability for any claim is capped at the total platform fees paid by the User in the three (3) months preceding the claim. Users agree to indemnify and hold Awa Foods & Groceries harmless against any losses arising from their use of the platform.</p>

                <h4 class="text-primary fw-black mb-3">6. JURISDICTION</h4>
                <p>Governed by the laws of the Federal Republic of Nigeria. Legal proceedings shall be directed to the courts of Port Harcourt, Rivers State.</p>
            </div>
        `
    });
};

const getPrivacyPolicy = (req, res) => {
    res.render('legal_pages', {
        title: 'Privacy Policy',
        content: `
            <div class="legal-standard">
                <h4 class="text-primary fw-black mb-3">1. DATA CONTROLLERS</h4>
                <p>Pikop and its Users act as independent data controllers under the Nigeria Data Protection Act (NDPA).</p>

                <h4 class="text-primary fw-black mb-3">2. DATA USE</h4>
                <p>We process GPS location, contact info, and KYC documents strictly to facilitate matching, real-time tracking, and platform security. We do not sell personal data.</p>

                <h4 class="text-primary fw-black mb-3">3. SECURITY</h4>
                <p>Users are responsible for maintaining the confidentiality of their account credentials. 2-Factor Authentication is recommended where available.</p>
            </div>
        `
    });
};

/**
 * Returns full legal configuration for the mobile app (Standardized HTML).
 */
const getLegalConfig = (req, res) => {
    res.status(200).json({
        success: true,
        terms_html: `
            <div style="font-family: sans-serif; line-height: 1.6; color: #333; padding: 15px;">
                <h2 style="color: #008751;">Terms & Conditions</h2>
                <p><em>Standardized Version 3.1 - Sep 2026</em></p>

                <h3 style="color: #008751;">1. Operational Model</h3>
                <p>Pikop is a technology intermediary and <strong>Commercial Agent</strong> for independent fulfillers. By using the app, you agree to our role as a connector and agent.</p>

                <h3 style="color: #008751;">2. Prohibited Items & Reporting</h3>
                <p>Illegal goods, drugs, and weapons are banned. <strong>Discoveries will be reported to authorities along with sender details. Prohibited items will be discarded immediately without refund.</strong></p>

                <h3 style="color: #008751;">3. Fees & Penalties</h3>
                <ul style="padding-left: 20px;">
                    <li><strong>Cancellation:</strong> 25% fee applies once matched. No cancellation after pickup.</li>
                    <li><strong>Recipient Absent:</strong> Non-refundable after 10-minute wait.</li>
                    <li><strong>Return Charge:</strong> 75% of original fare.</li>
                    <li><strong>Refunds:</strong> Issued as Wallet Credits only.</li>
                </ul>

                <h3 style="color: #008751;">4. Hold Harmless</h3>
                <p>Users agree to indemnify and hold Pikop harmless against any legal claims or losses. Liability is capped at the last 3 months of platform fees.</p>

                <h3 style="color: #008751;">5. Governing Law</h3>
                <p>Jurisdiction: Port Harcourt, Rivers State, Nigeria.</p>
            </div>
        `,
        privacy_html: `
            <div style="font-family: sans-serif; line-height: 1.6; color: #333; padding: 15px;">
                <h2 style="color: #008751;">Privacy Policy</h2>
                <p>We process your data (GPS, Contact, KYC) in strict compliance with the Nigeria Data Protection Act (NDPA).</p>
                <p>GPS data is used exclusively for mission tracking and agent matching.</p>
                <p>We do not share your private data with third parties for marketing purposes.</p>
            </div>
        `
    });
};

module.exports = { getTerms, getPrivacyPolicy, getLegalConfig };
