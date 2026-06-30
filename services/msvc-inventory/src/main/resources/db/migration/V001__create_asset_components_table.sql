CREATE TABLE IF NOT EXISTS asset_components (
    id UUID PRIMARY KEY,
    asset_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    location VARCHAR(255),
    observations TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_asset_components_asset
        FOREIGN KEY (asset_id)
            REFERENCES asset_table (id)
);

CREATE INDEX IF NOT EXISTS idx_asset_components_asset_id
    ON asset_components (asset_id);

