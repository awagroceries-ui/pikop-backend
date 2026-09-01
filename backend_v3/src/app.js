const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const http = require('http');
const path = require('path');
const fs = require('fs');
const session = require('express-session');
const expressLayouts = require('express-ejs-layouts');
const compression = require('compression');
const { rateLimit } = require('express-rate-limit');
require('express-async-errors');
require('dotenv').config();

const app = express();
const server = http.createServer(app);

// 0. Ensure Directories Exist
const UPLOADS_DIR = path.join(process.cwd(), 'uploads');
if (!fs.existsSync(UPLOADS_DIR)) {
  fs.mkdirSync(UPLOADS_DIR, { recursive: true });
  console.log(`✅ Created missing uploads directory at: ${UPLOADS_DIR}`);
}

// 0. Trust Proxy (Crucial for secure cookies behind Nginx)
app.set('trust proxy', 1);

// Initialize Sockets
const socketService = require('./services/socketService');
socketService.init(server);

// 1. Basic Middleware
app.use(compression()); // Optimize payload size
app.use(helmet({ contentSecurityPolicy: false }));
app.use(cors());
app.use(morgan('dev'));
app.use(express.json({
  verify: (req, res, buf) => {
    if (req.originalUrl.includes('/webhook')) {
      req.rawBody = buf.toString();
    }
  }
}));
app.use(express.urlencoded({ extended: true }));

// 1.1 Rate Limiting (Brute Force Protection)
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  limit: 100, // Limit each IP to 100 requests per `window`
  standardHeaders: 'draft-7',
  legacyHeaders: false,
  message: { success: false, message: 'Too many requests, please try again later.' }
});

app.use('/api/v1/auth', authLimiter);
app.use('/admin/login', authLimiter);

// 2. Session Middleware (For Admin)
app.use(session({
  secret: process.env.JWT_SECRET || 'pikop_admin_secret_v3',
  resave: false,
  saveUninitialized: false,
  name: 'pikop.sid',
  cookie: {
    secure: process.env.NODE_ENV === 'production',
    httpOnly: true,
    maxAge: 24 * 60 * 60 * 1000 // 24 hours
  }
}));

// 3. View Engine
const viewsPath = path.join(__dirname, 'views');
app.set('views', viewsPath);
app.set('view engine', 'ejs');
app.use(expressLayouts);
app.set('layout', 'layout');

console.log(`[System] Views directory set to: ${viewsPath}`);

// 4. Static Files
app.use('/public', express.static(path.join(__dirname, '../public')));

// 5. Routes
app.use('/admin', require('./routes/adminRoutes'));
app.use('/admin/reports', require('./routes/reportRoutes'));
app.use('/api/v1/auth', require('./routes/authRoutes'));
app.use('/api/v1/orders', require('./routes/orderRoutes'));
app.use('/api/v1/payments', require('./routes/paymentRoutes'));
app.use('/api/v1/fulfillers', require('./routes/fulfillerRoutes'));
app.use('/api/v1/support', require('./routes/supportRoutes'));
app.use('/api/v1/marketplace', require('./routes/marketplaceRoutes'));
app.use('/api/v1/kitchens', require('./routes/kitchenRoutes'));
app.use('/api/v1/wallets', require('./routes/walletRoutes'));
app.use('/api/v1/merchants', require('./routes/merchantRoutes'));
app.use('/api/v1/growth', require('./routes/growthRoutes'));
app.use('/api/v1/promo-codes', require('./routes/growthRoutes')); // Legacy Compatibility
app.use('/api/v1/places', require('./routes/placesRoutes'));
app.use('/api/v1/addresses', require('./routes/addressRoutes'));
app.use('/api/v1/settings', require('./routes/settingsRoutes'));
app.use('/api/v1/webhooks', require('./routes/webhookRoutes')); // Added for Prembly
app.use('/legal', require('./routes/legalRoutes'));

// 6. Health & Base Routes
app.get('/health', (req, res) => res.json({ status: 'UP', timestamp: new Date(), version: '3.0.0-core' }));
app.get('/', (req, res) => res.json({ message: 'Pikop V3 API - Professional Multi-Platform Logistics', status: 'ONLINE' }));

// 7. Global Error Handler (Harden Core)
app.use((err, req, res, next) => {
    console.error(`[Error] ${req.method} ${req.path} >>`, err.stack);

    const status = err.status || 500;
    const message = err.message || 'Internal Server Error';

    res.status(status).json({
        success: false,
        error: {
            code: status,
            message: process.env.NODE_ENV === 'production' ? 'An unexpected error occurred' : message,
            path: req.path
        }
    });
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, '0.0.0.0', () => {
    console.log(`\n🚀 PIKOP V3 API RESTORED`);
    console.log(`📡 Network: http://0.0.0.0:${PORT}`);
    console.log(`🕒 Mode: ${process.env.NODE_ENV || 'production'}\n`);
});

module.exports = app;
