/**
 * Serves ultra-comprehensive platform legal documents.
 * Standardized to industry leaders (Bolt/Uber).
 */
const getTerms = (req, res) => {
    res.render('legal_pages', {
        title: 'Terms & Conditions',
        content: `
            <div class="legal-standard" style="font-family: sans-serif; color: #333; line-height: 1.6;">
                <h4 class="text-primary fw-black mb-3">1. DEFINITIONS</h4>
                <p><strong>"Platform"</strong> means Pikop, including its website and mobile applications, operated by Awa Foods & Groceries.<br>
                <strong>"User"</strong> means any individual or business entity registered to request logistics services.<br>
                <strong>"Fulfiller"</strong> means independent third-party service providers (couriers, riders, or drivers).<br>
                <strong>"Mission"</strong> refers to a single logistics or delivery request successfully accepted on the Platform.</p>

                <h4 class="text-primary fw-black mb-3">2. THE PIKOP SERVICE</h4>
                <p>Pikop is a technology intermediary that connects Users with Fulfillers. <strong>Awa Foods & Groceries acts as a Commercial Agent for independent Fulfillers.</strong> We do not provide transportation services directly and are not a common carrier. When a User makes a payment through the Platform, the User’s legal obligation to pay the Fulfiller is considered satisfied.</p>

                <h4 class="text-primary fw-black mb-3">3. USER ELIGIBILITY & CONDUCT</h4>
                <p>Users must be at least 18 years of age. You are responsible for maintaining the confidentiality of your account credentials. You agree not to: (a) reverse engineer the app; (b) use the service for any fraudulent or illegal purpose; (c) attempt to bypass platform fees.</p>

                <h4 class="text-primary fw-black mb-3">4. PROHIBITED ITEMS & ENFORCEMENT</h4>
                <p>The transportation of illegal drugs, weapons, explosives, hazardous chemicals, or stolen property is strictly prohibited. <strong>Discovery of any prohibited items will result in the immediate reporting of the Sender to the appropriate law enforcement authorities. Furthermore, such items will be discarded or surrendered to the authorities immediately without any liability, refund, or compensation to the sender.</strong></p>

                <h4 class="text-primary fw-black mb-3">5. FINANCIAL POLICIES</h4>
                <ul>
                    <li><strong>Fare Split:</strong> Fulfillers receive 75% of the delivery fare. 25% is retained as platform commission.</li>
                    <li><strong>Cancellation (Pre-Pickup):</strong> A 25% penalty fee of the total fare applies if cancelled after an agent is matched but before pickup.</li>
                    <li><strong>No Cancellation (Post-Pickup):</strong> Cancellation is strictly prohibited once an item has been picked up.</li>
                    <li><strong>Recipient Absence:</strong> Fares are non-refundable if the recipient is absent after a mandatory 10-minute agent wait.</li>
                    <li><strong>Return Charges:</strong> Failed deliveries requiring return to sender are charged at 75% of the original mission fare.</li>
                    <li><strong>Refunds:</strong> Approved refunds are issued exclusively as non-withdrawable <strong>Pikop Wallet Credits</strong>.</li>
                </ul>

                <h4 class="text-primary fw-black mb-3">6. INTELLECTUAL PROPERTY</h4>
                <p>All rights, titles, and interests in the Platform, including software, logos, and designs, remain the exclusive property of Awa Foods & Groceries.</p>

                <h4 class="text-primary fw-black mb-3">7. LIMITATION OF LIABILITY</h4>
                <p>Pikop is provided on an "as is" and "as available" basis. To the maximum extent permitted by law, Pikop’s total liability for any claim shall be limited to the aggregate platform fees paid by the User in the three (3) months preceding the event giving rise to the claim.</p>

                <h4 class="text-primary fw-black mb-3">8. INDEMNIFICATION (HOLD HARMLESS)</h4>
                <p>You agree to indemnify and hold Awa Foods & Groceries, its directors, and independent Fulfillers harmless from any claims, losses, or legal expenses arising from your breach of these terms or your violation of any law or the rights of a third party.</p>

                <h4 class="text-primary fw-black mb-3">9. FORCE MAJEURE</h4>
                <p>Neither party shall be liable for delays or failures in performance resulting from causes beyond their reasonable control, including acts of God, civil unrest, or telecommunications outages.</p>

                <h4 class="text-primary fw-black mb-3">10. TERMINATION</h4>
                <p>Pikop reserves the right to suspend or terminate your access to the Platform at any time, without notice, for conduct that we believe violates these terms or is harmful to other users or the platform.</p>

                <h4 class="text-primary fw-black mb-3">11. SEVERABILITY</h4>
                <p>If any provision of these terms is found to be unenforceable, the remaining provisions shall remain in full force and effect.</p>

                <h4 class="text-primary fw-black mb-3">12. GOVERNING LAW & JURISDICTION</h4>
                <p>These terms are governed by the laws of the Federal Republic of Nigeria. Any legal proceedings shall be directed exclusively to the competent courts of <strong>Port Harcourt, Rivers State.</strong></p>
            </div>
        `
    });
};

const getPrivacyPolicy = (req, res) => {
    res.render('legal_pages', {
        title: 'Privacy Policy',
        content: `
            <div class="legal-standard" style="font-family: sans-serif; color: #333; line-height: 1.6;">
                <h4 class="text-primary fw-black mb-3">1. DATA CONTROLLER</h4>
                <p>Awa Foods & Groceries acts as the independent data controller for personal information processed through the Pikop platform, in compliance with the Nigeria Data Protection Act (NDPA).</p>

                <h4 class="text-primary fw-black mb-3">2. INFORMATION WE COLLECT</h4>
                <p>We collect: (a) Registration data (Name, Email, Phone); (b) KYC documents (for Fulfillers); (c) Real-time GPS location data (essential for tracking and matching); (d) Transactional and payment logs.</p>

                <h4 class="text-primary fw-black mb-3">3. PURPOSE OF PROCESSING</h4>
                <p>Data is used to facilitate matches, enable live tracking, process secure payments via Paystack, and ensure platform safety. We do not sell your personal data to third parties.</p>

                <h4 class="text-primary fw-black mb-3">4. DATA SHARING</h4>
                <p>Your name and location are shared with the assigned Fulfiller or User during an active mission. We may share data with law enforcement if required by Nigerian law or in the discovery of prohibited items.</p>

                <h4 class="text-primary fw-black mb-3">5. YOUR RIGHTS</h4>
                <p>Under the NDPA, you have the right to access, correct, or request the deletion of your personal data. Contact us at privacy@awa.name.ng for such requests.</p>

                <h4 class="text-primary fw-black mb-3">6. DATA SECURITY</h4>
                <p>We implement industry-standard encryption and security protocols to protect your data from unauthorized access or breaches.</p>
            </div>
        `
    });
};

/**
 * Returns full legal configuration for the mobile app (Industry Standard).
 */
const getLegalConfig = (req, res) => {
    res.status(200).json({
        success: true,
        terms_html: `
            <div style="font-family: sans-serif; line-height: 1.5; color: #333; padding: 15px;">
                <h2 style="color: #008751; margin-bottom: 5px;">Terms & Conditions</h2>
                <p style="font-size: 12px; color: #666; margin-bottom: 20px;"><em>Standardized Professional Version 3.5 - Sep 2026</em></p>

                <h3 style="color: #008751; font-size: 16px;">1. Operational Model</h3>
                <p>Pikop is a technology intermediary and <strong>Commercial Agent</strong> for independent fulfillers. Awa Foods does not provide transportation directly.</p>

                <h3 style="color: #008751; font-size: 16px;">2. Prohibited Items & Enforcement</h3>
                <p>Illegal goods, drugs, and weapons are banned. <strong>Discoveries will be reported to authorities along with sender details. Prohibited items will be discarded immediately without refund or liability.</strong></p>

                <h3 style="color: #008751; font-size: 16px;">3. Fees, Cancellations & Returns</h3>
                <ul style="padding-left: 20px; margin-bottom: 15px;">
                    <li><strong>25% Cancellation Fee:</strong> Applied if cancelled after an agent is matched (pre-pickup).</li>
                    <li><strong>No Cancellation:</strong> Permitted after an item has been picked up.</li>
                    <li><strong>75% Return Fee:</strong> Applied for failed deliveries needing return to sender.</li>
                    <li><strong>Refunds:</strong> Issued exclusively as Pikop Wallet Credits.</li>
                </ul>

                <h3 style="color: #008751; font-size: 16px;">4. Liability & Indemnification</h3>
                <p>Users agree to indemnify and hold Pikop harmless against any legal claims or losses. Liability is capped at the last 3 months of platform fees.</p>

                <h3 style="color: #008751; font-size: 16px;">5. Governing Law</h3>
                <p>Jurisdiction: Port Harcourt, Rivers State, Nigeria.</p>
                <hr style="border: 0; border-top: 1px solid #eee; margin: 20px 0;">
                <p style="font-size: 11px; color: #999;">By continuing to use this application, you agree to comply with all operational guidelines and financial policies stated above.</p>
            </div>
        `,
        privacy_html: `
            <div style="font-family: sans-serif; line-height: 1.5; color: #333; padding: 15px;">
                <h2 style="color: #008751; margin-bottom: 20px;">Privacy Policy</h2>
                <h3 style="font-size: 15px;">1. Data Collection</h3>
                <p>We collect GPS data, contact details, and KYC documents to ensure platform security and matching accuracy.</p>
                <h3 style="font-size: 15px;">2. NDPA Compliance</h3>
                <p>We process your information in accordance with the Nigeria Data Protection Act (NDPA).</p>
                <h3 style="font-size: 15px;">3. Security</h3>
                <p>Your data is protected using industry-standard encryption. We do not sell your personal data to third parties.</p>
            </div>
        `
    });
};

module.exports = { getTerms, getPrivacyPolicy, getLegalConfig };
