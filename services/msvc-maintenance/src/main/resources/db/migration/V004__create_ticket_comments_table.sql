CREATE TABLE IF NOT EXISTS ticket_comment_table (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL,
    author_id VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_ticket_comment_ticket
        FOREIGN KEY (ticket_id)
        REFERENCES ticket_table (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ticket_comment_ticket_id
    ON ticket_comment_table(ticket_id);

CREATE INDEX IF NOT EXISTS idx_ticket_comment_created_at
    ON ticket_comment_table(created_at);
