package com.qblog.knowledge;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class McpCallLog {
    private final JdbcTemplate jdbc;
    public void record(long tokenId, String tool, boolean success, long duration) {
        jdbc.update("INSERT INTO mcp_call_log(token_id,tool_name,success,duration_ms) VALUES(?,?,?,?)", tokenId, tool, success, duration);
    }
    public List<Map<String,Object>> recent() {
        return jdbc.queryForList("SELECT l.id, l.tool_name AS toolName, l.success, l.duration_ms AS durationMs, l.created_at AS createdAt, t.name AS tokenName FROM mcp_call_log l LEFT JOIN knowledge_api_token t ON t.id=l.token_id ORDER BY l.id DESC LIMIT 100");
    }
}
