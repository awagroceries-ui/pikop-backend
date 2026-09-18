const db = require('../config/db');

/**
 * Adds a new product to the vendor's catalog.
 */
const addProduct = async (req, res) => {
  const { vendor_id, name, price, stock_quantity, description, category, unit, nafdac_number, photo_url } = req.body;
  const userId = req.user.id;

  try {
    // Auth Check: User must own the vendor and be ACTIVE
    const vCheck = await db.query("SELECT id, status FROM vendors WHERE id = $1 AND user_id = $2", [vendor_id, userId]);
    if (vCheck.rows.length === 0) return res.status(403).json({ success: false, message: 'Unauthorized' });

    if (vCheck.rows[0].status !== 'active') {
        return res.status(403).json({ success: false, message: 'Merchant account is not yet active. Please complete verification.' });
    }

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

/**
 * Updates an existing product.
 */
const updateProduct = async (req, res) => {
  const { id } = req.params;
  const { name, price, stock_quantity, description, category, unit, nafdac_number, photo_url, active } = req.body;
  const userId = req.user.id;

  try {
    // Ownership check
    const check = await db.query(`
      SELECT p.id FROM products p
      JOIN vendors v ON v.id = p.vendor_id
      WHERE p.id = $1 AND v.user_id = $2
    `, [id, userId]);

    if (check.rows.length === 0) return res.status(403).json({ success: false, message: 'Unauthorized' });

    const { rows } = await db.query(
      `UPDATE products
       SET name = COALESCE($1, name),
           price = COALESCE($2, price),
           stock_quantity = COALESCE($3, stock_quantity),
           description = COALESCE($4, description),
           category = COALESCE($5, category),
           unit = COALESCE($6, unit),
           nafdac_number = COALESCE($7, nafdac_number),
           photo_url = COALESCE($8, photo_url),
           active = COALESCE($9, active)
       WHERE id = $10
       RETURNING *`,
      [name, price, stock_quantity, description, category, unit, nafdac_number, photo_url, active, id]
    );

    res.status(200).json({ success: true, data: rows[0] });
  } catch (error) {
    throw error;
  }
};

/**
 * Deletes a product.
 */
const deleteProduct = async (req, res) => {
  const { id } = req.params;
  const userId = req.user.id;

  try {
    const check = await db.query(`
      SELECT p.id FROM products p
      JOIN vendors v ON v.id = p.vendor_id
      WHERE p.id = $1 AND v.user_id = $2
    `, [id, userId]);

    if (check.rows.length === 0) return res.status(403).json({ success: false, message: 'Unauthorized' });

    await db.query("DELETE FROM products WHERE id = $1", [id]);
    res.status(200).json({ success: true, message: 'Product removed' });
  } catch (error) {
    throw error;
  }
};

module.exports = {
  addProduct,
  updateProduct,
  deleteProduct,
  getMarketplace,
  getVendorDetails
};
