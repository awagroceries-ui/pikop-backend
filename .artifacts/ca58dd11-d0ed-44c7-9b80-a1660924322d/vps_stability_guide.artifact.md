# VPS Stability Guide (1GB RAM Optimization)

Running a full stack (Nginx, Node.js, PostgreSQL) on **1GB of RAM** is challenging and is the direct cause of your "502 Bad Gateway" (OOM crashes). Use these configurations to ensure stability.

## 1. Create a Swap File (MANDATORY)
Since you have 25GB SSD, you should allocate 2GB to "Swap" memory. This acts as emergency RAM.

```bash
# Create a 2GB swap file
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile

# Make it permanent across reboots
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# Verify it's working
free -h
```

## 2. Optimize Node.js (via PM2)
By default, Node.js might try to use more memory than available. You should limit the V8 heap and tell PM2 to restart the app if it exceeds a certain threshold.

**Recommended PM2 Start Command:**
```bash
pm2 start src/server.js --name "pikop-api" \
  --max-memory-restart 400M \
  --node-args="--max-old-space-size=350"
```
*   `--max-memory-restart 400M`: Restarts the app automatically if it hits 400MB.
*   `--max-old-space-size=350`: Tells the Node.js garbage collector to be more aggressive once it hits 350MB.

## 3. Optimize PostgreSQL
PostgreSQL's default config might be too heavy for 1GB RAM. Edit your config file (usually `/etc/postgresql/14/main/postgresql.conf`):

```ini
# Reduce shared buffers (default is often too high for 1GB)
shared_buffers = 128MB
# Limit concurrent connections to save memory per-process
max_connections = 50
# Aggressive memory reclaiming
work_mem = 4MB
```
*After editing, run: `sudo systemctl restart postgresql`*

## 4. Monitor Memory in Real-Time
Use this command to see which service is "eating" your RAM:
```bash
# Sorted by memory usage
top -o %MEM
```

> [!WARNING]
> With 1GB RAM, avoid running `npm install` or `build` commands while the app is active. These processes are very memory-intensive and will likely crash your API or Database. Stop the app (`pm2 stop all`) before installing new packages.
