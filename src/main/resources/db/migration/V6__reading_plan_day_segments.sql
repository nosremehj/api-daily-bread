ALTER TABLE reading_plan_days ADD COLUMN segment_index INTEGER NOT NULL DEFAULT 0;
ALTER TABLE reading_plan_days DROP CONSTRAINT uq_reading_plan_days_plan_day;
ALTER TABLE reading_plan_days ADD CONSTRAINT uq_reading_plan_days_plan_day_seg UNIQUE (plan_id, day_number, segment_index);
