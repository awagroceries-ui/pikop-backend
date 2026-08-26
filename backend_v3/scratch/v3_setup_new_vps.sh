#!/bin/bash

# PIKOP V3 NEW VPS INFRASTRUCTURE SETUP
# TARGET OS: Ubuntu 22.04 LTS
# This script installs Node.js, PostgreSQL+PostGIS, Nginx, and PM2.

echo "🚀 PIKOP VPS INFRASTRUCTURE INJECTION INITIATED"

# 1. System Updates
echo "🔄 Updating system packages..."
sudo apt update && sudo apt upgrade -y

# 2. Essential Tools
echo "🛠️  Installing essential tools..."
sudo apt install -y build-essential git curl wget libpq-dev

# 3. Node.js (v20 LTS)
echo "📦 Installing Node.js v20..."
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
node -v

# 4. PostgreSQL & PostGIS
echo "🐘 Installing PostgreSQL and PostGIS..."
sudo apt install -y postgresql postgresql-contrib postgis postgresql-14-postgis-3
sudo systemctl start postgresql
sudo systemctl enable postgresql

# Create 'pikop' user and database
echo "🔑 Configuring Database User..."
sudo -u postgres psql -c "CREATE USER pikop WITH PASSWORD 'pikop_secure_v3';"
sudo -u postgres psql -c "CREATE DATABASE pikop OWNER pikop;"
sudo -u postgres psql -d pikop -c "CREATE EXTENSION IF NOT EXISTS postgis;"
echo "✅ Database 'pikop' created with PostGIS."

# 5. Nginx
echo "🌐 Installing Nginx..."
sudo apt install -y nginx
sudo systemctl start nginx
sudo systemctl enable nginx

# 6. PM2
echo "⚙️  Installing PM2 globally..."
sudo npm install -g pm2

# 7. Project Directory
echo "📂 Setting up project directories..."
sudo mkdir -p /var/www/pikop-api
sudo chown -R $USER:$USER /var/www/pikop-api

echo "✨ INFRASTRUCTURE SETUP COMPLETE."
echo "⚠️  NEXT STEPS:"
echo "1. SCP your database backup to this server."
echo "2. Clone your repository into /var/www/pikop-api/backend_v3."
echo "3. Configure your .env file."
