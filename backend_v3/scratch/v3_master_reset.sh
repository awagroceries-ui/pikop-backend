#!/bin/bash

# PIKOP V3 MASTER RESET & DEPLOYMENT (v3.0.10)
# This script wipes the DB, runs migrations, and seeds the admin.
# Note: Ensure your .env file is correctly configured before running.

echo "🛑 Stopping current PIKOP-V3 process..."
pm2 stop pikop-v3 || true

echo "🗑️  Wiping database (Total Reset)..."
sudo -u postgres psql -d pikop << 'EOF'
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO public;
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
EOF

echo "🏗️  Running v3 migrations..."
npm run migrate:up

echo "🌱 Seeding admin and knowledge base..."
node scratch/v3_seed_admin.js
node scratch/v3_seed_kb.js
node scratch/v3_seed_kitchens.js

echo "🚀 Restarting PIKOP-V3..."
pm2 start src/app.js --name "pikop-v3" --update-env || pm2 restart pikop-v3 --update-env

echo "✅ MASTER RESET COMPLETE."
