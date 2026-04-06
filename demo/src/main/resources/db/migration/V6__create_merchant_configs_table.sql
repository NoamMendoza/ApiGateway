CREATE TABLE merchant_configs (
    merchant_id VARCHAR(255) PRIMARY KEY,
    webhook_url VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
