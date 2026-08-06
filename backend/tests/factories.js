const db = require('../src/config/db');
const authService = require('../src/services/authService');

const createUser = async (overrides = {}) => {
  const defaultUser = {
    full_name: 'Test User',
    email: `test${Date.now()}@example.com`,
    phone: `+234${Math.floor(1000000000 + Math.random() * 9000000000)}`,
    password: 'password123'
  };
  const user = { ...defaultUser, ...overrides };
  const hash = await authService.hashPassword(user.password);

  const { rows } = await db.query(
    "INSERT INTO users (full_name, email, phone, password_hash) VALUES ($1, $2, $3, $4) RETURNING *",
    [user.full_name, user.email, user.phone, hash]
  );
  return rows[0];
};

const createFulfiller = async (userId, overrides = {}) => {
  const { rows } = await db.query(
    "INSERT INTO fulfillers (user_id, online_status, kyc_status) VALUES ($1, 'ONLINE', 'VERIFIED') RETURNING *",
    [userId]
  );
  return rows[0];
};

const createOrder = async (userId, overrides = {}) => {
  const defaultOrder = {
    pickup_address: '123 Pickup St',
    delivery_address: '456 Delivery Rd',
    total_fare: 1000.00,
    status: 'SEARCHING'
  };
  const order = { ...defaultOrder, ...overrides };

  const { rows } = await db.query(
    `INSERT INTO orders (user_id, pickup_location, delivery_location, pickup_address, delivery_address, status, total_fare)
     VALUES ($1, ST_GeographyFromText('POINT(0 0)'), ST_GeographyFromText('POINT(0 0)'), $2, $3, $4, $5)
     RETURNING *`,
    [userId, order.pickup_address, order.delivery_address, order.status, order.total_fare]
  );
  return rows[0];
};

module.exports = {
  createUser,
  createFulfiller,
  createOrder
};
