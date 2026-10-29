package com.qblog.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qblog.common.SecurityUtils;
import com.qblog.common.exception.BusinessException;
import com.qblog.entity.User;
import com.qblog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class KnowledgeTokenService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final UserService users;
    private static final SecureRandom RANDOM = new SecureRandom();
    public record DirectoryScope(boolean all, boolean inbox, Set<Long> directoryIds) {
        public DirectoryScope {
            directoryIds = directoryIds == null ? Set.of() : Set.copyOf(directoryIds);
        }
        public boolean allows(Long id) { return all || (id == null ? inbox : directoryIds.contains(id)); }
    }
    public record CreateRequest(String name, Instant expiresAt, Set<String> scopes, DirectoryScope directoryScope) {}
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    public record TokenView(Long id, String name, Instant expiresAt, Set<String> scopes,
                            DirectoryScope directoryScope, Instant createdAt, Instant revokedAt, String secret) {}
    public record Principal(Long id, Long userId, Set<String> scopes, DirectoryScope directoryScope) {
        public void require(String scope) {
            if (!scopes.contains(scope)) throw new BusinessException(403, "Tool access denied");
        }
    }
    public List<TokenView> list() {
        return jdbc.query("SELECT * FROM knowledge_api_token ORDER BY id DESC", (rs, n) -> view(rs, null));
    }
    @Transactional
    public TokenView create(CreateRequest request) {
        if (request == null || request.name() == null || request.name().isBlank() || request.name().length() > 100
                || request.expiresAt() == null || !request.expiresAt().isAfter(Instant.now())
                || request.expiresAt().isAfter(Instant.now().plus(365, ChronoUnit.DAYS))
                || request.scopes() == null || request.scopes().isEmpty()
                || !Set.of("READ", "WRITE", "ARTICLE_READ", "ARTICLE_WRITE", "ARTICLE_PUBLISH", "ARTICLE_DELETE").containsAll(request.scopes()) || request.directoryScope() == null)
            throw new BusinessException(400, "Invalid token configuration");
        DirectoryScope scope = request.directoryScope();
        if (((request.scopes().contains("READ") || request.scopes().contains("WRITE")) && !scope.all() && !scope.inbox() && scope.directoryIds().isEmpty())
                || (scope.all() && (scope.inbox() || !scope.directoryIds().isEmpty()))
                || scope.directoryIds().stream().anyMatch(id -> id == null || id <= 0))
            throw new BusinessException(400, "An explicit directory scope is required");
        Long owner = SecurityUtils.getCurrentUserId();
        User user = owner == null ? null : users.getById(owner);
        if (user == null || !Integer.valueOf(1).equals(user.getRole()) || !Integer.valueOf(1).equals(user.getStatus()))
            throw new BusinessException(403, "Admin JWT required");
        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
        String secret = "qnt_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = Instant.now();
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var ps = connection.prepareStatement("INSERT INTO knowledge_api_token (user_id,name,secret_hash,scopes,directory_scope,expires_at,created_at) VALUES (?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, owner); ps.setString(2, request.name().trim()); ps.setString(3, hash(secret));
            ps.setString(4, encode(request.scopes())); ps.setString(5, encode(scope));
            ps.setTimestamp(6, Timestamp.from(request.expiresAt())); ps.setTimestamp(7, Timestamp.from(now));
            return ps;
        }, key);
        return new TokenView(Objects.requireNonNull(key.getKey()).longValue(), request.name().trim(), request.expiresAt(),
                request.scopes(), scope, now, null, secret);
    }
    public void revoke(Long id) {
        jdbc.update("UPDATE knowledge_api_token SET revoked_at=COALESCE(revoked_at,CURRENT_TIMESTAMP(6)) WHERE id=?", id);
    }
    public Principal authenticate(String secret) {
        if (secret == null || !secret.matches("qnt_[A-Za-z0-9_-]{43}")) return null;
        var principals = jdbc.query("SELECT * FROM knowledge_api_token WHERE secret_hash=? AND revoked_at IS NULL AND expires_at>CURRENT_TIMESTAMP(6)",
                (rs,n) -> new Principal(rs.getLong("id"), rs.getLong("user_id"), decode(rs.getString("scopes"), new TypeReference<Set<String>>() {}),
                        decode(rs.getString("directory_scope"), new TypeReference<DirectoryScope>() {})), hash(secret));
        if (principals.size() != 1) return null;
        Principal p = principals.get(0);
        User owner = users.getById(p.userId());
        return owner != null && Integer.valueOf(1).equals(owner.getRole()) && Integer.valueOf(1).equals(owner.getStatus()) ? p : null;
    }
    private TokenView view(java.sql.ResultSet rs, String secret) throws java.sql.SQLException {
        Timestamp revoked = rs.getTimestamp("revoked_at");
        return new TokenView(rs.getLong("id"),rs.getString("name"),rs.getTimestamp("expires_at").toInstant(),
                decode(rs.getString("scopes"),new TypeReference<Set<String>>() {}), decode(rs.getString("directory_scope"),new TypeReference<DirectoryScope>() {}),
                rs.getTimestamp("created_at").toInstant(),revoked == null ? null : revoked.toInstant(),secret);
    }
    static String hash(String secret) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    private String encode(Object value) {
        try { return json.writeValueAsString(value); } catch (Exception e) { throw new IllegalStateException(e); }
    }
    private <T> T decode(String value, TypeReference<T> type) {
        try { return json.readValue(value, type); } catch (Exception e) { throw new IllegalStateException("Invalid token configuration",e); }
    }
}
