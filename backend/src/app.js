const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const path = require('path');
const fs = require('fs');
const session = require('express-session');
const expressLayouts = require('express-ejs-layouts');

const app = express();

// 0. Ensure Directories Exist
const UPLOADS_DIR = path.join(__dirname, '../uploads');
if (!fs.existsSync(UPLOADS_DIR)) {
  fs.mkdirSync(UPLOADS_DIR, { recursive: true });
  console.log('✅ Created missing uploads directory');
}

// 1. Version Check (For Troubleshooting)
const VERSION = '2.2.2-stable';
if (process.env.MASTER_OTP) {
    console.log(`✅ Master OTP feature is ACTIVE (Code: ${process.env.MASTER_OTP})`);
} else {
    console.warn('⚠️ MASTER_OTP not found in environment.');
}

// 2. Basic Middleware
app.use(helmet({ contentSecurityPolicy: false }));
app.use(cors());
app.use(morgan('dev'));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// 3. Session Middleware
app.use(session({
  secret: process.env.JWT_SECRET || 'pikop_admin_secret',
  resave: false,
  saveUninitialized: false,
  cookie: { secure: false }
}));

// 4. View Engine
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'ejs');
app.use(expressLayouts);
app.set('layout', 'layout');

// 5. STATIC FILES (Priority)
app.use('/public', express.static(path.join(__dirname, '../public')));
app.use('/uploads', express.static(path.join(__dirname, '../uploads')));

// 6. ADMIN ROUTES (High Priority)
app.use('/admin', require('./routes/adminRoutes'));

// 7. API v1 ROUTES
const { authenticateToken } = require('./middleware/authMiddleware');
app.use('/api/v1/auth', require('./routes/authRoutes'));
app.use('/api/v1/orders', require('./routes/orderRoutes'));
app.use('/api/v1/fulfillers', require('./routes/fulfillerRoutes'));
app.use('/api/v1/addresses', require('./routes/addressRoutes'));
app.use('/api/v1/wallets', require('./routes/walletRoutes'));
app.use('/api/v1/support', require('./routes/supportRoutes'));
app.use('/api/v1/payments', require('./routes/paymentRoutes'));
app.use('/api/v1/corporate', require('./routes/corporateRoutes'));
app.use('/api/v1/settings', require('./routes/settingsRoutes'));

// 8. PUBLIC ENDPOINTS
app.get('/health', (req, res) => res.json({ status: 'UP', version: VERSION }));
app.get('/', (req, res) => res.json({ message: `Pikop API v${VERSION}`, status: 'ONLINE' }));

// 9. GLOBAL ERROR HANDLER (Last Middleware)
app.use((err, req, res, next) => {
  console.error('--- INTERNAL SERVER ERROR ---');
  console.error('Path:', req.path);
  console.error('Stack:', err.stack);
  console.error('-----------------------------');
  res.status(500).json({
    error: 'Internal Server Error',
    message: err.message,
    path: req.path
  });
});

module.exports = app;
