#!/bin/bash

# PIKOP V3 DATABASE RESTORE TOOL
# Usage: bash scratch/v3_restore_db.sh path/to/backup.sql

BACKUP_FILE=$1

if [ -z "$BACKUP_FILE" ]; then
    echo "❌ ERROR: Please provide the path to the backup .sql file."
    echo "Usage: bash scratch/v3_restore_db.sh backups/pikop_v3_backup_2026-08-26.sql"
    exit 1
fi

if [ ! -f "$BACKUP_FILE" ]; then
    echo "❌ ERROR: File not found: $BACKUP_FILE"
    exit 1
fi

echo "⚠️  CAUTION: This will OVERWRITE the current 'pikop' database."
read -p "Are you sure? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "❌ Restore cancelled."
    exit 1
fi

echo "🐘 Dropping current database..."
sudo -u postgres dropdb pikop --if-exists

echo "🐘 Creating fresh database..."
sudo -u postgres createdb pikop -O pikop
sudo -u postgres psql -d pikop -c "CREATE EXTENSION IF NOT EXISTS postgis;"

echo "💾 Restoring data from $BACKUP_FILE..."
sudo -u postgres psql pikop < "$BACKUP_FILE"

if [ $? -eq 0 ]; then
    echo "✅ DATABASE RESTORE SUCCESSFUL."
else
    echo "❌ ERROR: Database restore failed."
    exit 1
fi
