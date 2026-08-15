#!/bin/bash

# PIKOP V3 NGINX FIX
# Replaces the current Nginx config with a guaranteed WebSocket-compatible one.

CONFIG_FILE=$(grep -l "api.awa.name.ng" /etc/nginx/conf.d/*.conf /etc/nginx/sites-enabled/* 2>/dev/null | head -n 1)

if [ -z "$CONFIG_FILE" ]; then
  echo "❌ Config file not found."
  exit 1
fi

echo "✅ Found config: $CONFIG_FILE"

# Create a clean version of the config
cat << 'EOF' > /tmp/pikop_nginx.conf
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

    listen 443 ssl; # managed by Certbot
    ssl_certificate /etc/letsencrypt/live/api.awa.name.ng/fullchain.pem; # managed by Certbot
    ssl_certificate_key /etc/letsencrypt/live/api.awa.name.ng/privkey.pem; # managed by Certbot
    include /etc/letsencrypt/options-ssl-nginx.conf; # managed by Certbot
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem; # managed by Certbot
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

# Move to the actual config path
mv /tmp/pikop_nginx.conf $CONFIG_FILE

# Test and reload
nginx -t && systemctl reload nginx

echo "🚀 Nginx fixed and reloaded."
