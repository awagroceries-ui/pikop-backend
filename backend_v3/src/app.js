const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const http = require('http');
require('express-async-errors'); // Automatic handling of async errors
require('dotenv').config();

const app = express();
const server = http.createServer(app);

// 1. Basic Middleware
app.use(helmet());
app.use(cors());
app.use(morgan('dev'));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// 2. Health & Base Routes
app.get('/health', (req, res) => res.json({ status: 'UP', timestamp: new Date(), version: '3.0.0-core' }));
app.get('/', (req, res) => res.json({ message: 'Pikop V3 API - Professional Multi-Platform Logistics', status: 'ONLINE' }));

// 3. API v1 Routes
app.use('/api/v1/auth', require('./routes/authRoutes'));

// 4. Global Error Handler (Harden Core)
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
