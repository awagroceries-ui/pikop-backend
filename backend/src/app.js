const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const path = require('path');
const session = require('express-session');
const expressLayouts = require('express-ejs-layouts');

const app = express();

// 1. Version Check (For Troubleshooting)
const VERSION = '1.3.4';

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
app.use('/api/v1/corporate', require('./routes/corporateRoutes'));
app.use('/api/v1/settings', require('./routes/settingsRoutes'));

// 8. PUBLIC ENDPOINTS
app.get('/health', (req, res) => res.json({ status: 'UP', version: VERSION }));
app.get('/', (req, res) => res.json({ message: `Pikop API v${VERSION}`, status: 'ONLINE' }));

module.exports = app;
