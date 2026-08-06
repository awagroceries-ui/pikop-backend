const request = require('supertest');
const app = require('../src/app');
const { clearDatabase } = require('./setup');
const db = require('../src/config/db');

beforeEach(async () => {
  await clearDatabase();
});

afterAll(async () => {
  await db.pool.end();
});

describe('Authentication Flow', () => {
  test('POST /api/v1/auth/signup should create a new user and OTP', async () => {
    const res = await request(app)
      .post('/api/v1/auth/signup')
      .send({
        full_name: 'John Doe',
        email: 'john@example.com',
        phone: '+2348000000001',
        password: 'password123'
      });

    expect(res.statusCode).toEqual(201);
    expect(res.body).toHaveProperty('accessToken');
    expect(res.body.message).toContain('User registered');

    // Verify DB
    const user = await db.query("SELECT * FROM users WHERE email = 'john@example.com'");
    expect(user.rows.length).toBe(1);

    const otp = await db.query("SELECT * FROM otp_verifications WHERE user_id = $1", [user.rows[0].id]);
    expect(otp.rows.length).toBe(1);
  });

  test('POST /api/v1/auth/login should return tokens for valid credentials', async () => {
    // Setup: Create user
    await request(app)
      .post('/api/v1/auth/signup')
      .send({
        full_name: 'Jane Doe',
        email: 'jane@example.com',
        phone: '+2348000000002',
        password: 'password123'
      });

    const res = await request(app)
      .post('/api/v1/auth/login')
      .send({
        email: 'jane@example.com',
        password: 'password123'
      });

    expect(res.statusCode).toEqual(200);
    expect(res.body).toHaveProperty('accessToken');
    expect(res.body).toHaveProperty('refreshToken');
  });

  test('POST /api/v1/auth/signup should be rate limited', async () => {
    const payload = {
      full_name: 'Rate Limit Test',
      email: 'rate@example.com',
      phone: '+2348000000003',
      password: 'password123'
    };

    // 5 allowed requests
    for (let i = 0; i < 5; i++) {
      await request(app).post('/api/v1/auth/signup').send({ ...payload, email: `test${i}@example.com`, phone: `+2348000000${10+i}` });
    }

    // 6th request should fail
    const res = await request(app).post('/api/v1/auth/signup').send(payload);
    expect(res.statusCode).toEqual(429);
  });
});
