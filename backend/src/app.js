const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const path = require('path');
const session = require('express-session');
const expressLayouts = require('express-ejs-layouts');

const app = express();

// View Engine Setup
app.use(expressLayouts);
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));
app.set('layout', 'layout');

// Middleware
app.use(helmet({
  contentSecurityPolicy: false, // Disable for alpha/bootstrap CDN
}));
app.use(cors());
app.use(morgan('dev'));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Session Setup
app.use(session({
  secret: process.env.JWT_SECRET || 'pikop_secret',
  resave: false,
  saveUninitialized: true,
  cookie: { secure: false } // Set true if using HTTPS/VPS
}));

// Health Check
app.get('/health', (req, res) => {
  res.status(200).json({ status: 'ok', timestamp: new Date().toISOString() });
});

// Routes
app.use('/api/v1/auth', require('./routes/authRoutes'));
app.use('/api/v1/orders', require('./routes/orderRoutes'));
app.use('/api/v1/withdrawals', require('./routes/withdrawalRoutes'));
app.use('/admin', require('./routes/adminRoutes'));

// Error handling middleware
app.use((err, req, res, next) => {
  console.error(err.stack);
  if (req.path.startsWith('/admin')) {
    return res.status(500).render('error', { message: 'Something went wrong on the dashboard.', admin: req.session?.adminUsername || 'Admin' });
  }
  res.status(500).json({ error: 'Internal Server Error' });
});

module.exports = app;
