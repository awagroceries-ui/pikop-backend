-- PIKOP V3 DATABASE INTEGRITY REPAIR
-- Manually ensures all critical columns exist in the orders table.

DO $$
BEGIN
    -- 1. Add quote_id if missing
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='orders' AND column_name='quote_id') THEN
        ALTER TABLE orders ADD COLUMN quote_id UUID REFERENCES quotes(id) ON DELETE SET NULL;
    END IF;

    -- 2. Add item_description if missing
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='orders' AND column_name='item_description') THEN
        ALTER TABLE orders ADD COLUMN item_description TEXT;
    END IF;

    -- 3. Add recipient fields if missing
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='orders' AND column_name='recipient_name') THEN
        ALTER TABLE orders ADD COLUMN recipient_name VARCHAR(255);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='orders' AND column_name='recipient_phone') THEN
        ALTER TABLE orders ADD COLUMN recipient_phone VARCHAR(20);
    END IF;

    -- 4. Add display summaries if missing
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='orders' AND column_name='pickup_display_summary') THEN
        ALTER TABLE orders ADD COLUMN pickup_display_summary TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='orders' AND column_name='delivery_display_summary') THEN
        ALTER TABLE orders ADD COLUMN delivery_display_summary TEXT;
    END IF;

    -- 5. Add photo url if missing
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='orders' AND column_name='item_photo_url') THEN
        ALTER TABLE orders ADD COLUMN item_photo_url TEXT;
    END IF;

    -- 6. Add size_tier if missing
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='orders' AND column_name='size_tier') THEN
        ALTER TABLE orders ADD COLUMN size_tier VARCHAR(20);
    END IF;

END $$;
