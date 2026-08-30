#!/bin/bash

# PIKOP V3 TOTAL RECOVERY & DIRECTORY ALIGNMENT (v4)
# This script flattens the nested folder trap and restores all keys.

PROJECT_ROOT="/var/www/pikop-api/backend_v3"

echo "🛑 Stopping all PM2 processes..."
pm2 delete all || true
sudo fuser -k 3000/tcp || true

echo "📂 Cleaning and Flattening Directory..."
cd $PROJECT_ROOT

# Flatten if nested
if [ -d "backend_v3" ]; then
    echo "🏗️  Detected nested folder. Performing DEEP FORCE MERGE..."
    # Force overwrite existing files with the new ones from Git
    cp -rf backend_v3/* .
    cp -rf backend_v3/.* . 2>/dev/null

    # Cleanup the nested folder to prevent future confusion
    rm -rf backend_v3
    echo "✅ Directory flattened and old nested folder removed."
fi

echo "📦 Reinstalling Node Dependencies (Force)..."
rm -rf node_modules package-lock.json
npm install --production

echo "🏗️  Synchronizing Database Schema..."
node -r dotenv/config ./node_modules/.bin/node-pg-migrate up

echo "🔧 Running Super System Repair..."
node scratch/super_restore.js

echo "🎟️  Re-activating TEST100 Coupon..."
node scratch/v3_seed_test_coupon.js

echo "🚀 Launching PIKOP V3 CORE..."
pm2 start src/app.js --name "pikop-v3" --update-env
pm2 save

echo "✅ RECOVERY SUCCESSFUL."
pm2 status
