#!/bin/bash

# PIKOP V3 EMERGENCY RECOVERY & STABILITY - v3.0.16
# Align with new domain: api.pikop.com.ng

DOMAIN="api.pikop.com.ng"

echo "🛑 Cleaning up existing processes..."
pm2 delete all || true
sudo fuser -k 3000/tcp || true

echo "📂 Resetting directory state..."
cd /var/www/pikop-api/backend_v3
rm -rf node_modules package-lock.json

echo "📦 Reinstalling all dependencies..."
npm install

echo "⚙️  Fixing Nginx for $DOMAIN..."
bash scratch/v3_fix_nginx.sh

echo "🚀 Starting PIKOP-V3..."
pm2 start src/app.js --name "pikop-v3" --update-env
pm2 save

echo "✅ EMERGENCY RECOVERY COMPLETE."
echo "Check your dashboard at https://$DOMAIN/admin/login"
