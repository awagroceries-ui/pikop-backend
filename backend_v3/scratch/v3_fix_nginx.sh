#!/bin/bash

# PIKOP V3 NGINX FIX (v3.0.13)
# Updates gateway for the new domain: api.pikop.com.ng

DOMAIN="api.pikop.com.ng"
CONFIG_FILE="/etc/nginx/conf.d/$DOMAIN.conf"

echo "✅ Target Config: $CONFIG_FILE"

# Create a bootstrap configuration (HTTP only first to allow Certbot to run)
# If certificates exist, it will include them.
CERT_PATH="/etc/letsencrypt/live/$DOMAIN/fullchain.pem"

if [ -f "$CERT_PATH" ]; then
  echo "🔒 SSL Certificates found. Generating SECURE config..."
  cat << EOF > /tmp/pikop_v3_nginx.conf
server {
    server_name $DOMAIN;

    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host \$host;
        proxy_cache_bypass \$http_upgrade;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    listen 443 ssl;
    ssl_certificate $CERT_PATH;
    ssl_certificate_key /etc/letsencrypt/live/$DOMAIN/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;
}

server {
    if (\$host = $DOMAIN) {
        return 301 https://\$host\$request_uri;
    }
    listen 80;
    server_name $DOMAIN;
    return 404;
}
EOF
else
  echo "⚡ No SSL found. Generating BOOTSTRAP config..."
  cat << EOF > /tmp/pikop_v3_nginx.conf
server {
    listen 80;
    server_name $DOMAIN;

    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
    }
}
EOF
fi

# Move and apply
sudo mv /tmp/pikop_v3_nginx.conf "$CONFIG_FILE"

echo "🧪 Testing Nginx configuration..."
if sudo nginx -t; then
    echo "✅ Syntax check passed. Reloading Nginx..."
    sudo systemctl reload nginx
    echo "🚀 PIKOP V3 Gateway Updated for $DOMAIN."
else
    echo "❌ ERROR: Nginx syntax check failed."
    exit 1
fi
