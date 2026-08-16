module.exports = {
  apps: [{
    name: 'pikop-v3',
    script: 'src/app.js',
    instances: 1, // Single instance for simplicity in V3 alpha, use 'max' for cluster
    autorestart: true,
    watch: false,
    max_memory_restart: '1G',
    env_production: {
      NODE_ENV: 'production',
      PORT: 3000
    }
  }]
};
