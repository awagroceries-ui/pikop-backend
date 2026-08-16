#!/bin/bash

# PIKOP V3 DATABASE BACKUP AUTOMATION
# Saves a timestamped dump of the 'pikop' database.

BACKUP_DIR="/var/www/pikop-api/backend_v3/backups"
TIMESTAMP=$(date +"%Y-%m-%d_%H-%M-%S")
FILENAME="pikop_v3_backup_${TIMESTAMP}.sql"

# Ensure backup directory exists
mkdir -p "$BACKUP_DIR"

echo "💾 Starting backup of 'pikop' database..."
sudo -u postgres pg_dump pikop > "${BACKUP_DIR}/${FILENAME}"

if [ $? -eq 0 ]; then
    echo "✅ Backup successful: ${FILENAME}"
    # Keep only the last 30 backups to save space
    ls -t "$BACKUP_DIR"/pikop_v3_backup_*.sql | tail -n +31 | xargs rm -- || true
else
    echo "❌ ERROR: Database backup failed."
    exit 1
fi
