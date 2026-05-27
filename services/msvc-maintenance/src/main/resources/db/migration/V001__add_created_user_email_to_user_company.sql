-- Add created_user_email column to user_company_table
ALTER TABLE user_company_table ADD COLUMN IF NOT EXISTS created_user_email VARCHAR(254);

