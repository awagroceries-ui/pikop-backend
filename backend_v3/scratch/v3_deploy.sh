#!/bin/bash

# PIKOP V3 AUTOMATED DEPLOYMENT
# Usage: bash scratch/v3_deploy.sh

echo "📦 Pulling latest code from GitHub..."
git fetch --all
git reset --hard origin/main

echo "🛠️  Installing dependencies..."
npm install --production

# --- ENVIRONMENT CHECK ---
if [ ! -f ".env" ]; then
    echo "❌ ERROR: .env file not found in $(pwd)"
    echo "Please ensure your secret keys are in the project root."
    exit 1
fi
# -------------------------

echo "🏗️  Running database migrations..."
node -r dotenv/config ./node_modules/.bin/node-pg-migrate up

echo "🧹 Clearing application cache..."
# Force Nginx to drop old static handles and PM2 to flush memory
sudo systemctl reload nginx
pm2 flush pikop-v3

echo "🔧 Running Super Repair script..."
node scratch/super_restore.js

echo "🚀 Restarting production process via PM2..."
pm2 restart ecosystem.config.js --env production

echo "✅ DEPLOYMENT COMPLETE."
pm2 status
