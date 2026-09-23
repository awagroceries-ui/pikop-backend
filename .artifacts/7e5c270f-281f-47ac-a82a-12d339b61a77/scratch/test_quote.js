const https = require('https');

function post(path, body, token) {
  return new Promise((resolve, reject) => {
    const data = JSON.stringify(body);
    const headers = {
      'Content-Type': 'application/json',
      'Content-Length': Buffer.byteLength(data)
    };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const req = https.request({
      hostname: 'api.pikop.com.ng',
      port: 443,
      path,
      method: 'POST',
      headers
    }, res => {
      let responseBody = '';
      res.on('data', chunk => responseBody += chunk);
      res.on('end', () => resolve({ status: res.statusCode, body: JSON.parse(responseBody || '{}') }));
    });

    req.on('error', reject);
    req.write(data);
    req.end();
  });
}

async function run() {
  try {
    const testEmail = `qtest_${Date.now()}@pikop.ng`;
    const signupRes = await post('/api/v1/auth/signup', {
      full_name: 'Quote Test User',
      email: testEmail,
      phone: `080${Math.floor(10000000 + Math.random() * 90000000)}`,
      password: 'Password123!',
      role: 'CUSTOMER'
    });

    console.log('SIGNUP STATUS:', signupRes.status);

    // Verify OTP using master OTP
    const verifyRes = await post('/api/v1/auth/verify-otp', {
      email: testEmail,
      otp: '123456'
    });
    console.log('VERIFY OTP STATUS:', verifyRes.status, verifyRes.body);

    const loginRes = await post('/api/v1/auth/login', {
      email: testEmail,
      password: 'Password123!'
    });
    console.log('LOGIN STATUS:', loginRes.status);
    const token = loginRes.body.accessToken;

    if (!token) {
      console.log('Login failed:', loginRes.body);
      return;
    }

    const quoteRes = await post('/api/v1/orders/quote', {
      delivery_address: "House 3 Road 1, Elelenwo, Elelenwa 501101, Rivers, Nigeria",
      delivery_landmark: "SonoCare Clinics ",
      delivery_lat: 4.825888397539672,
      delivery_lng: 7.081281244754791,
      initiator_role: "PAYER",
      item_description: "Perfume",
      item_price: 35000.0,
      pickup_address: "19 Old Aba Rd, Obia, Port Harcourt 500102, Rivers, Nigeria",
      pickup_landmark: "Mini Okoro police station ",
      pickup_lat: 4.83564000909597,
      pickup_lng: 7.040107659995556,
      pickup_state: "Rivers",
      recipient_phone: "+2349187563214"
    }, token);

    console.log('QUOTE STATUS:', quoteRes.status);
    console.log('QUOTE BODY:', JSON.stringify(quoteRes.body, null, 2));
  } catch (e) {
    console.error('ERROR:', e.message);
  }
}

run();
