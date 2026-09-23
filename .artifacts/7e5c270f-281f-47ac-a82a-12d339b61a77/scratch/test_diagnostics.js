const https = require('https');

function request(path, method, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null;
    const headers = { 'Content-Type': 'application/json' };
    if (data) headers['Content-Length'] = Buffer.byteLength(data);
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const req = https.request({
      hostname: 'api.pikop.com.ng',
      port: 443,
      path,
      method,
      headers
    }, res => {
      let responseBody = '';
      res.on('data', chunk => responseBody += chunk);
      res.on('end', () => {
        let parsed = responseBody;
        try { parsed = JSON.parse(responseBody); } catch (_) {}
        resolve({ status: res.statusCode, body: parsed });
      });
    });

    req.on('error', reject);
    if (data) req.write(data);
    req.end();
  });
}

async function run() {
  console.log('=== TEST: END-TO-END ORDER DISPATCH & OFFERS ===');

  // 1. Customer Signup & Order Creation
  const cEmail = `ctest_${Date.now()}@pikop.ng`;
  await request('/api/v1/auth/signup', 'POST', {
    full_name: 'Test Customer',
    email: cEmail,
    phone: `080${Math.floor(10000000 + Math.random() * 90000000)}`,
    password: 'Password123!',
    role: 'CUSTOMER'
  });
  await request('/api/v1/auth/verify-otp', 'POST', { email: cEmail, otp: '123456' });
  const cLogin = await request('/api/v1/auth/login', 'POST', { email: cEmail, password: 'Password123!' });
  const cToken = cLogin.body.accessToken;

  // Get Quote
  const quoteRes = await request('/api/v1/orders/quote', 'POST', {
    pickup_address: "19 Old Aba Rd, Port Harcourt, Rivers, Nigeria",
    delivery_address: "House 3 Road 1, Elelenwo, Rivers, Nigeria",
    pickup_lat: 4.8356,
    pickup_lng: 7.0401,
    delivery_lat: 4.8258,
    delivery_lng: 7.0812,
    item_description: "Documents",
    pickup_state: "Rivers"
  }, cToken);

  console.log('QUOTE STATUS:', quoteRes.status, 'QUOTE ID:', quoteRes.body.quote_id);

  // Create Order
  const orderRes = await request('/api/v1/orders', 'POST', {
    quote_id: quoteRes.body.quote_id,
    payment_method: 'promo',
    promo_id: null,
    recipient_name: 'Recipient Test',
    recipient_phone: '+2349187563214',
    pickup_lat: 4.8356,
    pickup_lng: 7.0401,
    delivery_lat: 4.8258,
    delivery_lng: 7.0812,
    pickup_display_summary: '19 Old Aba Rd',
    delivery_display_summary: 'House 3 Road 1',
    pickup_state: 'Rivers'
  }, cToken);

  console.log('ORDER STATUS:', orderRes.status, 'ORDER ID:', orderRes.body.order_id);

  // 2. Fulfiller Signup & Status Online
  const fEmail = `ftest_${Date.now()}@pikop.ng`;
  await request('/api/v1/auth/signup', 'POST', {
    full_name: 'Test Fulfiller',
    email: fEmail,
    phone: `080${Math.floor(10000000 + Math.random() * 90000000)}`,
    password: 'Password123!',
    role: 'FULFILLER'
  });
  await request('/api/v1/auth/verify-otp', 'POST', { email: fEmail, otp: '123456' });
  const fLogin = await request('/api/v1/auth/login', 'POST', { email: fEmail, password: 'Password123!' });
  const fToken = fLogin.body.accessToken;

  // Set Fulfiller Online (PATCH)
  const onlineRes = await request('/api/v1/fulfillers/status', 'PATCH', {
    online_status: 'ONLINE',
    lat: 4.8356,
    lng: 7.0401,
    current_state: 'Rivers'
  }, fToken);
  console.log('ONLINE STATUS:', onlineRes.status, onlineRes.body);

  // Fetch Offers
  const offersRes = await request('/api/v1/fulfillers/offers', 'GET', null, fToken);
  console.log('OFFERS STATUS:', offersRes.status);
  console.log('OFFERS RETURNED:', Array.isArray(offersRes.body) ? offersRes.body.length : offersRes.body);
  if (Array.isArray(offersRes.body) && offersRes.body.length > 0) {
    console.log('OFFER DETAILS:', offersRes.body[0]);
  }
}

run();
