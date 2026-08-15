const db = require('../config/db');

/**
 * Registers a new Kitchen.
 */
const registerKitchen = async (req, res) => {
  const { business_name, cac_number, contact_email, city, cuisine_type, description, state_food_safety_docs, bank_account_name, bank_account_number, bank_code, pickup_address_id } = req.body;
  const userId = req.user.id;

  try {
    const { rows } = await db.query(
      `INSERT INTO kitchens (business_name, cac_number, contact_email, city, cuisine_type, description, state_food_safety_docs, bank_account_name, bank_account_number, bank_code, pickup_address_id, user_id, status)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, 'pending')
       RETURNING id, business_name, status`,
      [business_name, cac_number, contact_email, city, cuisine_type, description, state_food_safety_docs, bank_account_name, bank_account_number, bank_code, pickup_address_id, userId]
    );

    res.status(201).json({
      success: true,
      message: 'Kitchen application submitted for review.',
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
 * Adds a new menu item.
 */
const addMenuItem = async (req, res) => {
  const { kitchen_id, name, price, description, category, photo_url, prep_time_minutes, modifiers } = req.body;
  const userId = req.user.id;

  try {
    // Auth Check: User must own the kitchen
    const kCheck = await db.query("SELECT id FROM kitchens WHERE id = $1 AND user_id = $2", [kitchen_id, userId]);
    if (kCheck.rows.length === 0) return res.status(403).json({ success: false, message: 'Unauthorized' });

    const { rows } = await db.query(
      `INSERT INTO menu_items (kitchen_id, name, price, description, category, photo_url, prep_time_minutes, modifiers)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
       RETURNING *`,
      [kitchen_id, name, price, description, category, photo_url, prep_time_minutes, modifiers]
    );

    res.status(201).json({ success: true, data: rows[0] });
  } catch (error) {
    throw error;
  }
};

/**
 * Fetches all active kitchens for customers.
 */
const getKitchens = async (req, res) => {
  const { cuisine_type, city } = req.query;

  try {
    let query = `
      SELECT k.*, a.formatted_address as pickup_address
      FROM kitchens k
      LEFT JOIN addresses a ON a.id = k.pickup_address_id
      WHERE k.status = 'active'
    `;
    const params = [];

    if (cuisine_type) {
      params.push(cuisine_type);
      query += ` AND k.cuisine_type = $${params.length}`;
    }

    if (city) {
      params.push(city);
      query += ` AND k.city = $${params.length}`;
    }

    query += " ORDER BY k.created_at DESC";

    const { rows } = await db.query(query, params);
    res.status(200).json({ success: true, data: rows });
  } catch (error) {
    throw error;
  }
};

/**
 * Fetches kitchen details and menu.
 */
const getKitchenDetails = async (req, res) => {
  const { id } = req.params;
  try {
    const kitchen = await db.query("SELECT * FROM kitchens WHERE id = $1", [id]);
    if (kitchen.rows.length === 0) return res.status(404).json({ success: false, message: 'Kitchen not found' });

    const menu = await db.query("SELECT * FROM menu_items WHERE kitchen_id = $1 AND available = true", [id]);

    res.status(200).json({
      success: true,
      data: {
        ...kitchen.rows[0],
        menu: menu.rows
      }
    });
  } catch (error) {
    throw error;
  }
};

module.exports = {
  registerKitchen,
  addMenuItem,
  getKitchens,
  getKitchenDetails
};
