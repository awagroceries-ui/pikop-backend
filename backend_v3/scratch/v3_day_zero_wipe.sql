-- PIKOP V3 DAY ZERO WIPE
-- CAUTION: This will permanently delete all data and schema for a fresh v3 start.

DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO public;

-- Enable PostGIS for new schema
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- End of Wipe Script
