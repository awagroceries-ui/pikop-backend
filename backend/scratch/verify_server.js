const app = require('../src/app');

// Check if app is correctly exported
if (typeof app === 'function') {
  console.log('Express app correctly exported.');
} else {
  console.error('Express app export failed.');
  process.exit(1);
}

// Check if routes are reachable (internal test)
const routerFound = app._router.stack.some(layer => layer.regexp.toString().includes('orders'));
if (routerFound) {
  console.log('Order routes registered.');
} else {
  console.error('Order routes missing.');
  process.exit(1);
}

console.log('Verification successful.');
