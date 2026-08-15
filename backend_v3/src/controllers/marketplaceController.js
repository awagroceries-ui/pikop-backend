const db = require('../config/db');

/**
 * Registers a new Vendor.
 */
const registerVendor = async (req, res) => {
  const { business_name, cac_number, contact_email, city, pickup_address_id, description, bank_account_name, bank_account_number, bank_code } = req.body;
  const userId = req.user.id;

  try {
    const { rows } = await db.query(
      `INSERT INTO vendors (business_name, cac_number, contact_email, city, pickup_address_id, description, bank_account_name, bank_account_number, bank_code, user_id, status)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, 'pending')
       RETURNING id, business_name, status`,
      [business_name, cac_number, contact_email, city, pickup_address_id, description, bank_account_name, bank_account_number, bank_code, userId]
    );

    res.status(201).json({
      success: true,
      message: 'Vendor application submitted for review.',
      data: rows[0]
    });
  } catch (error) {
    if (error.code === '23505') {
        return res.status(400).json({ success: false, message: 'CAC number already registered' });
    }
    throw error;
  }
};

/**
 * Adds a new product to the vendor's catalog.
 */
const addProduct = async (req, res) => {
  const { vendor_id, name, price, stock_quantity, description, category, unit, nafdac_number, photo_url } = req.body;
  const userId = req.user.id;

  try {
    // Auth Check: User must own the vendor
    const vCheck = await db.query("SELECT id FROM vendors WHERE id = $1 AND user_id = $2", [vendor_id, userId]);
    if (vCheck.rows.length === 0) return res.status(403).json({ success: false, message: 'Unauthorized' });

    const { rows } = await db.query(
      `INSERT INTO products (vendor_id, name, price, stock_quantity, description, category, unit, nafdac_number, photo_url)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
       RETURNING *`,
      [vendor_id, name, price, stock_quantity, description, category, unit, nafdac_number, photo_url]
    );

    res.status(201).json({ success: true, data: rows[0] });
  } catch (error) {
    throw error;
  }
};

/**
 * Fetches all active products in the marketplace.
 */
const getMarketplace = async (req, res) => {
    const { category, city } = req.query;

    try {
        let query = `
            SELECT p.*, v.business_name, v.city as vendor_city
            FROM products p
            JOIN vendors v ON v.id = p.vendor_id
            WHERE p.active = true AND v.status = 'active'
        `;
        const params = [];

        if (category) {
            params.push(category);
            query += ` AND p.category = $${params.length}`;
        }

        if (city) {
            params.push(city);
            query += ` AND v.city = $${params.length}`;
        }

        query += " ORDER BY p.created_at DESC";

        const { rows } = await db.query(query, params);
        res.status(200).json({ success: true, data: rows });
    } catch (error) {
        throw error;
    }
};

/**
 * Fetches vendor details and their products.
 */
const getVendorDetails = async (req, res) => {
    const { id } = req.params;
    try {
        const vendor = await db.query("SELECT * FROM vendors WHERE id = $1", [id]);
        if (vendor.rows.length === 0) return res.status(404).json({ success: false, message: 'Vendor not found' });

        const products = await db.query("SELECT * FROM products WHERE vendor_id = $1 AND active = true", [id]);

        res.status(200).json({
            success: true,
            data: {
                ...vendor.rows[0],
                products: products.rows
            }
        });
    } catch (error) {
        throw error;
    }
};

module.exports = {
  registerVendor,
  addProduct,
  getMarketplace,
  getVendorDetails
};
