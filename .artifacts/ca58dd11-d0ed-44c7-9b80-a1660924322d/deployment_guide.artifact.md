# Pikop VPS Deployment Guide

Follow these simplified steps to push your backend code to your TrueHost VPS and get it running.

## Step 1: Initialize Git (Local Machine)
If you haven't already, set up a Git repository for your project. Open your local terminal in the project root (`C:\Users\MOSES\AndroidStudioProjects\Pikop`):

```bash
git init
git add .
git commit -m "Initial commit: Pikop Backend and Android Onboarding"
```

> [!TIP]
> Push your code to a private repository on **GitHub** or **GitLab** first. This makes it much easier to "pull" the code onto your VPS.

---

## Step 2: Connect to your VPS (SSH)
Open your terminal and log in to your TrueHost VPS:

```bash
ssh root@your_vps_ip
```

---

## Step 3: Initial Setup on VPS
Once logged in, clone your repository and install dependencies:

```bash
# Clone the repository
git clone https://github.com/your-username/pikop.git
cd pikop/backend

# Install dependencies
npm install

# Create your environment file
cp .env.example .env  # Or manually create it
nano .env
```

> [!IMPORTANT]
> In the `.env` file, make sure to update:
> - `DATABASE_URL` (your Postgres credentials)
> - `PAYSTACK_SECRET_KEY` (your sk_live_...)
> - `GEMINI_API_KEY` (your Google AI key)

---

## Step 4: Database Migrations
Create your tables and enable PostGIS:

```bash
npm run migrate:up
```

---

## Step 5: Start the App with PM2
Use PM2 to keep your app running 24/7, even after you log out:

```bash
# Install PM2 globally if not already present
npm install -g pm2

# Start the app
pm2 start src/server.js --name pikop-api

# Ensure it starts on VPS reboot
pm2 startup
pm2 save
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
   cd pikop/backend
   git pull origin main
   npm install      # Only if you added new packages
   npm run migrate:up # Only if you changed the database
   pm2 restart pikop-api
   ```

---

## Troubleshooting
- **Check Logs**: `pm2 logs pikop-api`
- **Check Status**: `pm2 status`
- **Restart App**: `pm2 restart pikop-api`
- **Verify API**: `curl http://localhost:3000/health`
