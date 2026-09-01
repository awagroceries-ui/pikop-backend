# Pikop VPS Deployment Guide

Follow these simplified steps to push your backend code to your TrueHost VPS and get it running.

## Project Information (Detected)
- **VPS Path**: `/var/www/pikop-api`
- **PM2 Process Name**: `pikop-v3`

---

## Step 1: Initialize Git (Local Machine)
If you haven't already, set up a Git repository for your project. Open your local terminal in the project root (`C:\Users\MOSES\AndroidStudioProjects\Pikop`):

```bash
git add .
git commit -m "Update message"
git push origin main
```

---

## Step 2: Connect to your VPS (SSH)
Open your terminal and log in to your TrueHost VPS:

```bash
ssh root@your_vps_ip
```

---

## Step 3: Initial Setup on VPS (If starting fresh)
If you need to clone the project for the first time on a new server:

```bash
cd /var/www
git clone https://github.com/awagroceries-ui/pikop-backend.git pikop-api
cd pikop-api/backend_v3
npm install
cp .env.example .env
# Edit .env with your credentials
nano .env
```

---

## Step 6: How to Update (The "Push" Flow)
Whenever you make changes in Android Studio and want to see them on the VPS:

1. **On your Local Machine**:
   ```bash
   git add .
   git commit -m "Update message"
   git push origin main
   ```

2. **On your VPS**:
   ```bash
   # Go to the backend directory where Git is initialized
   cd /var/www/pikop-api/backend_v3

   # Pull the latest changes
   git pull origin main

   # Restart the backend service
   pm2 restart pikop-v3
   ```

---

## Troubleshooting
- **Check Logs**: `pm2 logs pikop-v3`
- **Check Status**: `pm2 status`
- **Restart App**: `pm2 restart pikop-v3`
- **Verify API**: `curl http://localhost:3000/health`
