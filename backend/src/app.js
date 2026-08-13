const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const path = require('path');
const session = require('express-session');
const expressLayouts = require('express-ejs-layouts');

const app = express();

// 1. Basic Middleware
app.use(helmet({ contentSecurityPolicy: false }));
app.use(cors());
app.use(morgan('dev'));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// 2. Session Middleware (MUST be before routes)
app.use(session({
  secret: process.env.JWT_SECRET || 'pikop_admin_secret',
  resave: false,
  saveUninitialized: false,
  cookie: { secure: false }
}));

// 3. View Engine Configuration
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'ejs');
app.use(expressLayouts);
app.set('layout', 'layout');

// 4. Admin Dashboard Routes
const adminRoutes = require('./routes/adminRoutes');
app.use('/admin', adminRoutes);

// 5. Static Files
app.use('/public', express.static(path.join(__dirname, '../public')));
app.use('/uploads', express.static(path.join(__dirname, '../uploads')));

// 6. Public Health/Root
app.get('/', (req, res) => res.json({ message: 'Pikop API v1.3.1', status: 'ONLINE' }));
app.get('/health', (req, res) => res.json({ status: 'UP', timestamp: new Date().toISOString() }));

// 7. API v1 Routes
const { authenticateToken } = require('./middleware/authMiddleware');
const promoController = require('./controllers/promoController');
const trackingController = require('./controllers/trackingController');

app.get('/track/:token', trackingController.getPublicTracking);

app.use('/api/v1/auth', require('./routes/authRoutes'));
app.use('/api/v1/orders', require('./routes/orderRoutes'));
app.use('/api/v1/fulfillers', require('./routes/fulfillerRoutes'));
app.use('/api/v1/addresses', require('./routes/addressRoutes'));
app.use('/api/v1/wallets', require('./routes/walletRoutes'));
app.use('/api/v1/support', require('./routes/supportRoutes'));
app.use('/api/v1/corporate', require('./routes/corporateRoutes'));
app.use('/api/v1/settings', require('./routes/settingsRoutes'));
app.post('/api/v1/promo-codes/validate', authenticateToken, promoController.validateCode);

module.exports = app;
