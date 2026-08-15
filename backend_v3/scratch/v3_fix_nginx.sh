#!/bin/bash

# PIKOP V3 NGINX FIX (v3.0.12)
# This script completely rebuilds the Nginx config for api.awa.name.ng
# to resolve duplicate directives and enable WebSocket support.

DOMAIN="api.awa.name.ng"
CONFIG_FILE=$(grep -l "$DOMAIN" /etc/nginx/conf.d/*.conf /etc/nginx/sites-enabled/* 2>/dev/null | head -n 1)

if [ -z "$CONFIG_FILE" ]; then
  echo "❌ Config file for $DOMAIN not found. Creating a new one in /etc/nginx/conf.d/api.awa.name.ng.conf..."
  CONFIG_FILE="/etc/nginx/conf.d/api.awa.name.ng.conf"
fi

echo "✅ Target Config: $CONFIG_FILE"

# Backup the current config
cp "$CONFIG_FILE" "${CONFIG_FILE}.bak"
echo "💾 Backup created at ${CONFIG_FILE}.bak"

# Create a clean, validated configuration
cat << 'EOF' > /tmp/pikop_v3_nginx.conf
server {
    server_name api.awa.name.ng;

    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # SSL Configuration (Managed by Certbot)
    listen 443 ssl;
    ssl_certificate /etc/letsencrypt/live/api.awa.name.ng/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.awa.name.ng/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;
}

server {
    if ($host = api.awa.name.ng) {
        return 301 https://$host$request_uri;
    }
    listen 80;
    server_name api.awa.name.ng;
    return 404;
}
EOF

# Move and apply
mv /tmp/pikop_v3_nginx.conf "$CONFIG_FILE"

echo "🧪 Testing Nginx configuration..."
if nginx -t; then
    echo "✅ Syntax check passed. Reloading Nginx..."
    systemctl reload nginx
    echo "🚀 PIKOP V3 Gateway Restored."
else
    echo "❌ ERROR: Nginx syntax check failed. Reverting to backup..."
    mv "${CONFIG_FILE}.bak" "$CONFIG_FILE"
    exit 1
fi
