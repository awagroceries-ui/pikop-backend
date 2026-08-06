const request = require('supertest');
const app = require('../src/app');
const { clearDatabase } = require('./setup');
const { createUser, createFulfiller, createOrder } = require('./factories');
const db = require('../src/config/db');
const authService = require('../src/services/authService');

let user, fulfiller, token, fulfillerToken;

beforeEach(async () => {
  await clearDatabase();

  // Create a verified user
  user = await createUser({ email_verified_at: new Date() });
  const userTokens = authService.generateTokens(user);
  token = userTokens.accessToken;

  // Create a fulfiller
  const fUser = await createUser({ email: 'f1@example.com', phone: '+2348000000111' });
  fulfiller = await createFulfiller(fUser.id);
  const fTokens = authService.generateTokens(fUser);
  fulfillerToken = fTokens.accessToken;
});

afterAll(async () => {
  await db.pool.end();
});

describe('Order Lifecycle & Race Conditions', () => {
  test('Fulfiller can accept an order atomically', async () => {
    const order = await createOrder(user.id);

    const res = await request(app)
      .post(`/api/v1/orders/${order.id}/accept`)
      .set('Authorization', `Bearer ${fulfillerToken}`)
      .send({ fulfillerId: fulfiller.id });

    expect(res.statusCode).toEqual(200);
    expect(res.body.status).toBe('MATCHED');

    // Verify DB
    const updatedOrder = await db.query("SELECT * FROM orders WHERE id = $1", [order.id]);
    expect(updatedOrder.rows[0].fulfiller_id).toBe(fulfiller.id);
  });

  test('Race Condition: Only one fulfiller can accept the same order', async () => {
    const order = await createOrder(user.id);

    // Create another fulfiller
    const f2User = await createUser({ email: 'f2@example.com', phone: '+2348000000222' });
    const fulfiller2 = await createFulfiller(f2User.id);
    const f2Tokens = authService.generateTokens(f2User);
    const fulfiller2Token = f2Tokens.accessToken;

    // Send simultaneous requests
    const [res1, res2] = await Promise.all([
      request(app).post(`/api/v1/orders/${order.id}/accept`).set('Authorization', `Bearer ${fulfillerToken}`).send({ fulfillerId: fulfiller.id }),
      request(app).post(`/api/v1/orders/${order.id}/accept`).set('Authorization', `Bearer ${fulfiller2Token}`).send({ fulfillerId: fulfiller2.id })
    ]);

    // One must succeed, one must fail with 400
    const results = [res1.statusCode, res2.statusCode];
    expect(results).toContain(200);
    expect(results).toContain(400);

    const errorMsg = [res1.body.error, res2.body.error];
    expect(errorMsg).toContain('Order already accepted or cancelled');
  });

  test('Order transitions: PICKED_UP -> DELIVERED', async () => {
    // 1. Create order with codes
    const { rows } = await db.query(
      `INSERT INTO orders (user_id, pickup_location, delivery_location, pickup_address, delivery_address, status, total_fare, pickup_code, delivery_code)
       VALUES ($1, ST_GeographyFromText('POINT(0 0)'), ST_GeographyFromText('POINT(0 0)'), 'P', 'D', 'MATCHED', 1000, '1111', '2222')
       RETURNING *`,
      [user.id]
    );
    const order = rows[0];

    // 2. Verify Pickup
    const pickupRes = await request(app)
      .post(`/api/v1/orders/${order.id}/pickup`)
      .set('Authorization', `Bearer ${fulfillerToken}`)
      .send({ code: '1111' });
    expect(pickupRes.statusCode).toBe(200);

    // 3. Verify Delivery
    const deliveryRes = await request(app)
      .post(`/api/v1/orders/${order.id}/deliver`)
      .set('Authorization', `Bearer ${fulfillerToken}`)
      .send({ code: '2222' });
    expect(deliveryRes.statusCode).toBe(200);

    // 4. Verify Wallet credit (Fulfiller should get 750)
    const walletRes = await db.query("SELECT balance FROM wallets WHERE owner_id = $1 AND owner_type = 'FULFILLER'", [fulfiller.id]);
    expect(parseFloat(walletRes.rows[0].balance)).toBe(750.00);

    // 5. Verify Platform credit (250)
    const platformRes = await db.query("SELECT balance FROM wallets WHERE owner_type = 'PLATFORM'");
    expect(parseFloat(platformRes.rows[0].balance)).toBe(250.00);
  });
});
