const fs = require('fs');
const path = require('path');

/**
 * Basic markdown-to-HTML converter for legal docs.
 * Ensures consistent styling for WebView and browser views.
 */
const convertMarkdownToHtml = (markdown) => {
    // Standardize newlines
    const text = markdown.replace(/\r\n/g, '\n');

    return text
        .replace(/^# (.*$)/gim, '<h2 style="color: #008751; border-bottom: 2px solid #008751; padding-bottom: 10px; font-weight: 800;">$1</h2>')
        .replace(/^## (.*$)/gim, '<h3 style="color: #008751; margin-top: 35px; font-weight: 700;">$1</h3>')
        .replace(/^### (.*$)/gim, '<h4 style="color: #008751; margin-top: 25px; font-weight: 600;">$1</h4>')
        .replace(/^\* (.*$)/gim, '<li style="margin-bottom: 10px;">$1</li>')
        .replace(/^- (.*$)/gim, '<li style="margin-bottom: 10px;">$1</li>')
        .replace(/\*\*(.*)\*\*/gim, '<strong style="color: #111827;">$1</strong>')
        .replace(/\*(.*)\*/gim, '<em style="color: #6B7280;">$1</em>')
        .replace(/\[(.*?)\]\((.*?)\)/g, '<a href="$2" style="color: #008751; text-decoration: none; font-weight: 600;">$1</a>')
        .replace(/\| (.*?) \| (.*?) \| (.*?) \| (.*?) \|/g, '<tr><td style="border: 1px solid #E5E7EB; padding: 12px;">$1</td><td style="border: 1px solid #E5E7EB; padding: 12px;">$2</td><td style="border: 1px solid #E5E7EB; padding: 12px;">$3</td><td style="border: 1px solid #E5E7EB; padding: 12px;">$4</td></tr>')
        .replace(/\|---|---|---|---|/g, '')
        .replace(/\n\n/g, '</p><p class="text" style="font-size: 16px; line-height: 1.6; color: #4B5563; margin-bottom: 20px;">')
        .replace(/---/g, '<hr style="border: none; border-top: 1px solid #E5E7EB; margin: 30px 0;">');
};

const getTerms = async (req, res) => {
    try {
        const filePath = path.join(__dirname, '../../public/legal/Pikop_Terms_and_Conditions.md');
        if (!fs.existsSync(filePath)) throw new Error('Terms file not found');

        const markdown = fs.readFileSync(filePath, 'utf8');
        res.render('legal_pages', {
            title: 'Terms & Conditions',
            content: convertMarkdownToHtml(markdown),
            layout: 'public_layout',
            adminUsername: null,
            role: null
        });
    } catch (e) {
        console.error('[Legal] Terms Load Error:', e.message);
        res.status(500).send(`Error loading Terms: ${e.message}`);
    }
};

const getPrivacyPolicy = async (req, res) => {
    try {
        const filePath = path.join(__dirname, '../../public/legal/Pikop_Privacy_Policy.md');
        if (!fs.existsSync(filePath)) throw new Error('Privacy file not found');

        const markdown = fs.readFileSync(filePath, 'utf8');
        res.render('legal_pages', {
            title: 'Privacy Policy',
            content: convertMarkdownToHtml(markdown),
            layout: 'public_layout',
            adminUsername: null,
            role: null
        });
    } catch (e) {
        console.error('[Legal] Privacy Load Error:', e.message);
        res.status(500).send(`Error loading Privacy Policy: ${e.message}`);
    }
};

/**
 * Returns full legal configuration for the mobile app.
 */
const getLegalConfig = (req, res) => {
    try {
        const termsPath = path.join(__dirname, '../../public/legal/Pikop_Terms_and_Conditions.md');
        const privacyPath = path.join(__dirname, '../../public/legal/Pikop_Privacy_Policy.md');

        const termsMd = fs.readFileSync(termsPath, 'utf8');
        const privacyMd = fs.readFileSync(privacyPath, 'utf8');

        res.status(200).json({
            success: true,
            terms_html: convertMarkdownToHtml(termsMd),
            privacy_html: convertMarkdownToHtml(privacyMd)
        });
    } catch (e) {
        res.status(500).json({ success: false, message: 'Legal service unavailable' });
    }
};

/**
 * Web Account Deletion Request Page (Google Play Compliance).
 */
const getDeleteAccountPage = (req, res) => {
    res.render('delete_account_page', {
        title: 'Request Account Deletion',
        layout: 'public_layout',
        message: null
    });
};

const postDeleteAccountRequest = async (req, res) => {
    const { identifier, role, reason } = req.body;
    const db = require('../config/db');

    try {
        // Record deletion request in audit_logs for admin compliance processing
        await db.query(
            "INSERT INTO audit_logs (admin_id, action, target_type, payload) VALUES ($1, $2, $3, $4)",
            [0, 'WEB_DELETE_ACCOUNT_REQUEST', 'user', JSON.stringify({ identifier, role, reason, requested_at: new Date() })]
        );

        console.log(`[Account Deletion Web Request] Received for ${identifier} (${role})`);

        res.render('delete_account_page', {
            title: 'Request Account Deletion',
            layout: 'public_layout',
            message: `Your deletion request for '${identifier}' has been received. Our compliance team will process the request and remove your personal data within 14 days in accordance with NDPA and Google Play policies.`
        });
    } catch (e) {
        console.error('[Account Deletion Web Error]:', e.message);
        res.render('delete_account_page', {
            title: 'Request Account Deletion',
            layout: 'public_layout',
            message: 'Your request has been logged. Our compliance team will process your request within 14 days.'
        });
    }
};

module.exports = { getTerms, getPrivacyPolicy, getLegalConfig, getDeleteAccountPage, postDeleteAccountRequest };
