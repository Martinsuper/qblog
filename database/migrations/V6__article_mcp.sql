-- Apply once after backing up the database.
ALTER TABLE article ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
CREATE TABLE IF NOT EXISTS mcp_call_log (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, token_id BIGINT NOT NULL,
 tool_name VARCHAR(80) NOT NULL, success BOOLEAN NOT NULL, duration_ms BIGINT NOT NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 KEY idx_mcp_call_log_time (created_at), KEY idx_mcp_call_log_token (token_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
