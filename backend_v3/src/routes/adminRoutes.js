const express = require('express');
const router = express.Router();
const adminController = require('../controllers/adminController');

router.get('/login', (req, res) => res.render('login', { layout: false }));
router.post('/login', adminController.login);

router.get('/logout', (req, res) => {
    req.session.destroy();
    res.redirect('/admin/login');
});

// Dashboard
router.get('/dashboard', adminController.getDashboard);

module.exports = router;
