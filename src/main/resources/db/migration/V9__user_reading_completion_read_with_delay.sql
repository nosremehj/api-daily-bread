ALTER TABLE user_reading_completions
    ADD COLUMN read_with_delay BOOLEAN NOT NULL DEFAULT FALSE;
