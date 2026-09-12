/**
 * Serves ultra-comprehensive platform legal documents.
 * Standardized to industry leaders (Bolt/Uber/Amazon).
 */
const getTerms = (req, res) => {
    res.render('legal_pages', {
        title: 'Terms & Conditions',
        content: `
            <div class="legal-standard" style="font-family: 'Inter', sans-serif; color: #1a1a1a; line-height: 1.8; text-align: justify;">
                <h2 style="color: #008751; border-bottom: 2px solid #008751; padding-bottom: 10px;">MASTER SERVICE AGREEMENT</h2>
                <p><strong>Effective Date: September 12, 2026</strong></p>
                <p>This Master Service Agreement ("Agreement") constitutes a legally binding contract between you ("User", "Customer", or "Sender") and <strong>Awa Foods & Groceries</strong> ("Company", "Pikop", "We", or "Us"), a company registered under the laws of the Federal Republic of Nigeria.</p>

                <h4 class="text-primary fw-black mt-4 mb-3">1. DEFINITIONS AND INTERPRETATION</h4>
                <p><strong>1.1 "Platform"</strong> refers to the Pikop mobile application, website (https://pikop.com.ng), and all related technology services provided by the Company.<br>
                <strong>1.2 "Fulfiller"</strong> refers to independent third-party logistics providers, including but not limited to walking agents, bicycle riders, motorcycle riders, and vehicle drivers, who provide delivery services via the Platform.<br>
                <strong>1.3 "Mission"</strong> means any delivery or logistics request initiated by a User and accepted by a Fulfiller via the Platform.<br>
                <strong>1.4 "Recipient"</strong> means the person or entity designated by the User to receive the items being transported.</p>

                <h4 class="text-primary fw-black mt-4 mb-3">2. NATURE OF THE PIKOP SERVICE</h4>
                <p><strong>2.1 Technology Intermediary:</strong> Pikop is a technology platform that facilitates the connection between individuals/entities seeking delivery services and independent Fulfillers. <strong>Pikop does not provide transportation or logistics services directly.</strong><br>
                <strong>2.2 Commercial Agency:</strong> The Company acts as a limited <strong>Commercial Agent</strong> for Fulfillers for the sole purpose of collecting payments from Users. Your payment to Pikop shall be deemed a payment directly to the Fulfiller.</p>

                <h4 class="text-primary fw-black mt-4 mb-3">3. USER ELIGIBILITY & CONDUCT</h4>
                <p><strong>3.1 Eligibility:</strong> You must be at least 18 years of age to use the Platform. By using the Platform, you represent and warrant that you have the right, authority, and capacity to enter into this Agreement.<br>
                <strong>3.2 Account Security:</strong> You are responsible for maintaining the confidentiality of your account credentials and for all activities that occur under your account.</p>

                <h4 class="text-primary fw-black mt-4 mb-3">4. PROHIBITED ITEMS, MANDATORY REPORTING & DISPOSAL</h4>
                <p><strong>4.1 Strict Prohibition:</strong> Users are strictly prohibited from utilizing the Platform to transport any item that is illegal, hazardous, or restricted under the laws of Nigeria. This includes, but is not limited to: narcotics, psychotropic substances, weapons, ammunition, explosives, radioactive materials, live animals, stolen goods, and high-value currency.<br>
                <strong>4.2 Discovery & Law Enforcement:</strong> If any item in transit is discovered or suspected to be a Prohibited Item, <strong>Pikop and the Fulfiller reserve the absolute right to immediately report the Sender, the Recipient, and the item to the Nigeria Police Force or relevant regulatory authorities.</strong><br>
                <strong>4.3 Disposal without Liability:</strong> Upon discovery, <strong>Prohibited Items will be discarded, destroyed, or surrendered to the authorities immediately.</strong> The User explicitly waives any right to a refund, compensation, or legal claim against Pikop or the Fulfiller in relation to the disposal of such items. You shall be liable for any fines or legal costs incurred by the Company as a result of your attempt to send prohibited goods.</p>

                <h4 class="text-primary fw-black mt-4 mb-3">5. FINANCIAL POLICIES & PENALTIES</h4>
                <p><strong>5.1 Pricing:</strong> Fares are calculated dynamically based on distance, item size, and environmental factors. 75% of the delivery fare is allocated to the Fulfiller.<br>
                <strong>5.2 Cancellation Penalty:</strong> If a User cancels a Mission after a Fulfiller has been matched but before the item is picked up, a <strong>25% Cancellation Penalty</strong> of the original fare will be automatically deducted from the User's wallet. <strong>Cancellation is strictly prohibited once the Fulfiller has marked the item as "Picked Up."</strong><br>
                <strong>5.3 Recipient Absence:</strong> If the Fulfiller arrives at the destination and the Recipient is unavailable after a mandatory 10-minute wait, the Mission is considered fulfilled. <strong>The fare is strictly non-refundable.</strong><br>
                <strong>5.4 Return Fee:</strong> A return Mission to the original pickup point will attract an additional charge of <strong>75% of the original fare.</strong><br>
                <strong>5.5 Refunds:</strong> All approved refunds are issued exclusively as non-withdrawable <strong>Pikop Wallet Credits</strong>.</p>

                <h4 class="text-primary fw-black mt-4 mb-3">6. INTELLECTUAL PROPERTY</h4>
                <p>All software, code, logos, trademarks, and user interface designs are the exclusive property of Awa Foods & Groceries. Any unauthorized reproduction or reverse engineering is a violation of law.</p>

                <h4 class="text-primary fw-black mt-4 mb-3">7. LIMITATION OF LIABILITY</h4>
                <p><strong>7.1 "As Is" Basis:</strong> The Platform is provided without warranties of any kind. We do not guarantee that the service will be uninterrupted or error-free.<br>
                <strong>7.2 Liability Cap:</strong> To the maximum extent permitted by law, Pikop’s total cumulative liability to you for any cause whatsoever will be limited to the total platform fees actually paid by you to Pikop in the <strong>three (3) months</strong> preceding the event giving rise to the claim.</p>

                <h4 class="text-primary fw-black mt-4 mb-3">8. INDEMNIFICATION (HOLD HARMLESS)</h4>
                <p><strong>8.1 User Indemnity:</strong> You agree to <strong>indemnify, defend, and hold harmless</strong> Awa Foods & Groceries, its parent company, directors, employees, and independent Fulfillers from and against any and all claims, damages, liabilities, losses, costs, and expenses (including reasonable attorney fees) arising out of or in connection with: (i) your use of the Platform; (ii) any breach of this Agreement; (iii) the content or nature of any item you send via the Platform; or (iv) your violation of the rights of any third party.</p>

                <h4 class="text-primary fw-black mt-4 mb-3">9. FORCE MAJEURE</h4>
                <p>Neither party shall be liable for delays or failures in performance resulting from causes beyond their reasonable control, including acts of God, civil unrest, or telecommunications outages.</p>

                <h4 class="text-primary fw-black mt-4 mb-3">10. TERMINATION</h4>
                <p>Pikop reserves the right to suspend or terminate your access to the Platform at any time, without notice, for conduct that we believe violates these terms or is harmful to other users or the platform.</p>

                <h4 class="text-primary fw-black mt-4 mb-3">11. SEVERABILITY</h4>
                <p>If any provision of these terms is found to be unenforceable, the remaining provisions shall remain in full force and effect.</p>

                <h4 class="text-primary fw-black mt-4 mb-3">12. GOVERNING LAW AND JURISDICTION</h4>
                <p>This Agreement is governed by the laws of the Federal Republic of Nigeria. You agree that any dispute or legal proceeding shall be instituted exclusively in the competent courts of <strong>Port Harcourt, Rivers State.</strong></p>
            </div>
        `
    });
};

const getPrivacyPolicy = (req, res) => {
    res.render('legal_pages', {
        title: 'Privacy Policy',
        content: `
            <div class="legal-standard" style="font-family: 'Inter', sans-serif; color: #1a1a1a; line-height: 1.8; text-align: justify;">
                <h2 style="color: #008751; border-bottom: 2px solid #008751; padding-bottom: 10px;">PRIVACY POLICY</h2>
                <p><strong>Last Updated: September 12, 2026</strong></p>
                <p>This Privacy Policy describes how <strong>Awa Foods & Groceries</strong> processes personal data in compliance with the Nigeria Data Protection Act (NDPA).</p>

                <h4 class="text-primary fw-black mt-4 mb-3">1. DATA CONTROLLER</h4>
                <p>Awa Foods & Groceries is the independent data controller responsible for your personal information. We prioritize the security and confidentiality of all data processed on the Pikop platform.</p>

                <h4 class="text-primary fw-black mt-4 mb-3">2. INFORMATION WE COLLECT</h4>
                <p><strong>2.1 Registration Data:</strong> Name, verified email address, and phone number.<br>
                <strong>2.2 Geo-Location Data:</strong> Precise real-time GPS coordinates of Senders, Recipients, and Fulfillers during active Missions. This is mandatory for our dispatch and tracking services.<br>
                <strong>2.3 KYC Data:</strong> Identification documents, vehicle registration, and biometric photos (for Fulfillers).<br>
                <strong>2.4 Transactional Data:</strong> Payment references, order history, and chat logs.</p>

                <h4 class="text-primary fw-black mt-4 mb-3">3. HOW WE SHARE YOUR DATA</h4>
                <p><strong>3.1 Operational Sharing:</strong> We share your name and location with the assigned Fulfiller to facilitate your delivery. Fulfillers’ data is similarly shared with Senders and Recipients.<br>
                <strong>3.2 Legal Obligations:</strong> We will disclose personal data and item descriptions to the <strong>Nigeria Police Force or relevant authorities</strong> upon discovery of Prohibited Items or under a valid court order.<br>
                <strong>3.3 Third-Party Processors:</strong> Financial data is handled exclusively by Paystack. We do not sell your personal data to third parties for marketing purposes.</p>

                <h4 class="text-primary fw-black mt-4 mb-3">4. DATA SECURITY AND RETENTION</h4>
                <p>We implement bank-grade encryption to secure your data. We retain your information for as long as your account is active or as required by Nigerian financial regulations (typically 7 years for transactional logs).</p>

                <h4 class="text-primary fw-black mt-4 mb-3">5. YOUR RIGHTS</h4>
                <p>Under the NDPA, you have the right to: (i) Access your data; (ii) Request rectification of errors; (iii) Request deletion of your account and data; and (iv) Object to certain processing activities. Contact us at <strong>privacy@awa.name.ng</strong>.</p>
            </div>
        `
    });
};

/**
 * Returns full legal configuration for the mobile app (Ultra-Detailed Standard).
 */
const getLegalConfig = (req, res) => {
    // Shared styling for better rendering in Android WebView
    const sharedStyle = 'style="font-family: sans-serif; line-height: 1.6; color: #1a1a1a; padding: 20px; background: #fff;"';
    const headerStyle = 'style="color: #008751; font-weight: 800; border-bottom: 1px solid #eee; padding-bottom: 8px; margin-bottom: 20px;"';
    const sectionStyle = 'style="color: #008751; font-size: 17px; font-weight: 700; margin-top: 25px; margin-bottom: 10px;"';

    const termsHtml = `
        <div ${sharedStyle}>
            <h2 ${headerStyle}>MASTER SERVICE AGREEMENT</h2>
            <p style="font-size: 12px; color: #666; margin-bottom: 25px;">Standardized Professional Framework v3.9 (Sep 2026)</p>

            <h3 ${sectionStyle}>1. NATURE OF SERVICE</h3>
            <p>Pikop is a technology intermediary and <strong>Commercial Agent</strong> for independent fulfillers. Awa Foods & Groceries does not provide transport directly. Your payment to Pikop satisfies your obligation to the independent agent.</p>

            <h3 ${sectionStyle}>2. PROHIBITED ITEMS & ENFORCEMENT</h3>
            <p>The transport of illegal drugs, weapons, explosives, or stolen property is strictly prohibited. <strong>ANY DISCOVERY WILL BE REPORTED TO THE NIGERIA POLICE FORCE ALONG WITH SENDER IDENTITY. PROHIBITED ITEMS WILL BE DISCARDED OR SURRENDERED IMMEDIATELY WITHOUT REFUND OR LIABILITY.</strong></p>

            <h3 ${sectionStyle}>3. HOLD HARMLESS & INDEMNITY</h3>
            <p><strong>You agree to indemnify, defend, and hold Awa Foods & Groceries and its independent agents harmless from any legal claims, damages, or expenses arising from your use of the platform or the discovery of prohibited items.</strong></p>

            <h3 ${sectionStyle}>4. FEES & PENALTIES</h3>
            <ul style="padding-left: 20px;">
                <li><strong>Cancellation:</strong> 25% penalty applies once an agent is matched. Cancellation is strictly prohibited after pickup.</li>
                <li><strong>Recipient Absent:</strong> Fare is non-refundable after a mandatory 10-minute agent wait at destination.</li>
                <li><strong>Returns:</strong> Charged at 75% of the original mission fare.</li>
                <li><strong>Refunds:</strong> Issued exclusively as non-withdrawable Pikop Wallet Credits.</li>
            </ul>

            <h3 ${sectionStyle}>5. LIMITATION OF LIABILITY</h3>
            <p>To the maximum extent permitted by law, total platform liability is capped at the fees paid by you in the last three (3) months.</p>

            <h3 ${sectionStyle}>6. GOVERNING LAW</h3>
            <p>Governed by the laws of Nigeria. Exclusive Jurisdiction: <strong>Port Harcourt, Rivers State.</strong> By continuing, you agree to these comprehensive terms.</p>
        </div>
    `;

    const privacyHtml = `
        <div ${sharedStyle}>
            <h2 ${headerStyle}>PRIVACY POLICY</h2>
            <h3 ${sectionStyle}>1. DATA CONTROL</h3>
            <p>Awa Foods & Groceries is the data controller under the Nigeria Data Protection Act (NDPA).</p>
            <h3 ${sectionStyle}>2. COLLECTION & GPS</h3>
            <p>We process Registration data, KYC documents, and mandatory real-time GPS location data for mission accuracy and matching.</p>
            <h3 ${sectionStyle}>3. DISCLOSURE</h3>
            <p>Data is shared with agents to facilitate delivery. <strong>Sender identity will be disclosed to law enforcement upon discovery of prohibited items.</strong></p>
            <h3 ${sectionStyle}>4. DATA SUBJECT RIGHTS</h3>
            <p>You may request data access, rectification, or deletion via privacy@awa.name.ng.</p>
        </div>
    `;

    res.status(200).json({
        success: true,
        terms_html: termsHtml,
        privacy_html: privacyHtml
    });
};

module.exports = { getTerms, getPrivacyPolicy, getLegalConfig };
