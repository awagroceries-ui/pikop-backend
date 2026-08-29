#!/bin/bash

# PIKOP V3 NUCLEAR ALIGNMENT & RESTORATION (v3)
# This script flattens the directory, fixes keys, and restores services.

PROJECT_ROOT="/var/www/pikop-api/backend_v3"

echo "🛑 Stopping all processes..."
pm2 delete all || true
sudo fuser -k 3000/tcp || true

echo "📂 Cleaning directory structure..."
cd $PROJECT_ROOT

# If we are in the nested trap, move everything up
if [ -d "backend_v3" ]; then
    echo "🏗️  Detected nested folder. Moving files to root..."
    cp -rf backend_v3/* .
    cp -rf backend_v3/.* . 2>/dev/null
fi

# Ensure .env is in the root
if [ ! -f ".env" ] && [ -f "backend_v3/.env" ]; then
    mv backend_v3/.env ./.env
fi

echo "📦 Reinstalling dependencies..."
rm -rf node_modules package-lock.json
npm install --production

echo "🏗️  Running migrations..."
node -r dotenv/config ./node_modules/.bin/node-pg-migrate up

echo "🔧 Running Super Repair..."
node scratch/super_restore.js

echo "🎟️  Running System Integrity Tool..."
node scratch/v3_system_integrity.js

echo "🚀 Starting PIKOP-V3..."
pm2 start src/app.js --name pikop-v3 --update-env
pm2 save

echo "✅ SYSTEM RESTORED."
pm2 status
