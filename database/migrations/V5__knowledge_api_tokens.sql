-- Secrets are generated from 256 random bits. Only their SHA-256 digest is stored.
CREATE TABLE IF NOT EXISTS knowledge_api_token (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    secret_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    scopes JSON NOT NULL,
    directory_scope JSON NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    revoked_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_knowledge_token_hash (secret_hash),
    KEY idx_knowledge_token_owner (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
