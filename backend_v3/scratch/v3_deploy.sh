#!/bin/bash

# PIKOP V3 AUTOMATED DEPLOYMENT
# Usage: bash scratch/v3_deploy.sh

echo "📦 Pulling latest code from GitHub..."
git fetch --all
git reset --hard origin/main

echo "🛠️  Installing dependencies..."
npm install --production

echo "🏗️  Running database migrations..."
npm run migrate:up

echo "🚀 Restarting production process via PM2..."
pm2 restart ecosystem.config.js --env production

echo "✅ DEPLOYMENT COMPLETE."
pm2 status
