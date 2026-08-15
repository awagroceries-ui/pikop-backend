#!/bin/bash

# PIKOP V3 EMERGENCY RECOVERY & STABILITY - v3.0.15
# This script ensures the environment is clean, dependencies are correct,
# and the gateway is properly configured.

echo "🛑 Cleaning up existing processes..."
pm2 delete all || true
fuser -k 3000/tcp || true

echo "📂 Resetting directory state..."
cd /var/www/pikop-api/backend_v3
rm -rf node_modules package-lock.json

echo "📦 Reinstalling all dependencies (Force)..."
npm install express express-ejs-layouts ejs socket.io axios bcryptjs jsonwebtoken dotenv helmet morgan express-session node-pg-migrate pg uuid winston @google/generative-ai express-async-errors

echo "⚙️  Fixing Nginx (Guaranteed Clean)..."
CONFIG_FILE=$(grep -l "api.awa.name.ng" /etc/nginx/conf.d/*.conf /etc/nginx/sites-enabled/* 2>/dev/null | head -n 1)
cat << 'EOF' > /tmp/pikop_v3_clean.conf
server {
    server_name api.awa.name.ng;
    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    listen 443 ssl;
    ssl_certificate /etc/letsencrypt/live/api.awa.name.ng/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.awa.name.ng/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;
}
server {
    if ($host = api.awa.name.ng) { return 301 https://$host$request_uri; }
    listen 80;
    server_name api.awa.name.ng;
    return 404;
}
EOF
mv /tmp/pikop_v3_clean.conf $CONFIG_FILE
nginx -t && systemctl reload nginx

echo "🚀 Starting PIKOP-V3..."
pm2 start src/app.js --name "pikop-v3" --update-env
pm2 save

echo "✅ EMERGENCY RECOVERY COMPLETE."
echo "Check your dashboard at https://api.awa.name.ng/admin/login"
