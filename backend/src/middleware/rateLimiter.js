const rateLimit = {};

/**
 * Simple in-memory rate limiter to prevent OTP abuse.
 * Limits by IP address.
 */
const otpRateLimiter = (req, res, next) => {
  const ip = req.ip;
  const now = Date.now();
  const windowMs = 15 * 60 * 1000; // 15 minutes
  const maxRequests = 5;

  if (!rateLimit[ip]) {
    rateLimit[ip] = { count: 1, firstRequest: now };
  } else {
    const timePassed = now - rateLimit[ip].firstRequest;
    if (timePassed < windowMs) {
      rateLimit[ip].count++;
      if (rateLimit[ip].count > maxRequests) {
        return res.status(429).json({ error: 'Too many OTP requests. Please try again later.' });
      }
    } else {
      rateLimit[ip] = { count: 1, firstRequest: now };
    }
  }

  next();
};

module.exports = {
  otpRateLimiter
};
