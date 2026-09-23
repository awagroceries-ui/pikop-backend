const https = require('https');

function get(path) {
  return new Promise((resolve, reject) => {
    https.get({
      hostname: 'api.pikop.com.ng',
      port: 443,
      path,
    }, res => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => resolve({ status: res.statusCode, bodyLength: data.length }));
    }).on('error', reject);
  });
}

async function run() {
  console.log('Testing /legal/terms/fulfiller:', await get('/legal/terms/fulfiller'));
  console.log('Testing /terms/fulfiller:', await get('/terms/fulfiller'));
}

run();
