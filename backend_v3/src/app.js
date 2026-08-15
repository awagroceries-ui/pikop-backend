const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const http = require('http');
const path = require('path');
const session = require('express-session');
const expressLayouts = require('express-ejs-layouts');
require('express-async-errors');
require('dotenv').config();

const app = express();
const server = http.createServer(app);

// 0. Trust Proxy (Crucial for secure cookies behind Nginx)
app.set('trust proxy', 1);

// Initialize Sockets
const socketService = require('./services/socketService');
socketService.init(server);

// 1. Basic Middleware
app.use(helmet({ contentSecurityPolicy: false }));
app.use(cors());
app.use(morgan('dev'));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

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
app.use('/api/v1/auth', require('./routes/authRoutes'));
app.use('/api/v1/orders', require('./routes/orderRoutes'));
app.use('/api/v1/payments', require('./routes/paymentRoutes'));
app.use('/api/v1/fulfillers', require('./routes/fulfillerRoutes'));
app.use('/api/v1/support', require('./routes/supportRoutes'));
app.use('/api/v1/marketplace', require('./routes/marketplaceRoutes'));
app.use('/api/v1/kitchens', require('./routes/kitchenRoutes'));

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
server.listen(PORT, () => {
    console.log(`\n🚀 PIKOP V3 API RESTORED`);
    console.log(`📡 Endpoint: http://localhost:${PORT}`);
    console.log(`🕒 Mode: ${process.env.NODE_ENV || 'development'}\n`);
});

module.exports = app;
