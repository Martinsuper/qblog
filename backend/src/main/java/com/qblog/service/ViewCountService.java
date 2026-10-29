package com.qblog.service;

import com.qblog.mapper.ArticleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis pending batches survive ordinary DB failures/restarts. Delivery is
 * at-least-once, NOT exactly-once: a crash/ambiguous result after DB commit but
 * before Redis acknowledgement can duplicate a batch. Exactly-once needs durable
 * batch IDs and idempotent DB transactions/schema support. Local fallback is
 * process-memory only and cannot survive process termination.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViewCountService {
    private final ArticleMapper articleMapper;
    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private static final String CURRENT = "article:views:";
    private static final String PENDING = "article:viewpending:";
    private static final String LOCK = "article:viewlock:";
    private static final String DEDUP = "article:viewdedup:";
    private static final long DEDUP_WINDOW_MS = Duration.ofHours(24).toMillis();
    private final Map<Long, Long> localViewCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> localViewDedup = new ConcurrentHashMap<>();

    // Atomically rotate only when there is no unacknowledged batch.
    private static final DefaultRedisScript<String> DRAIN = new DefaultRedisScript<>(
            "if redis.call('GET',KEYS[3])~=ARGV[1] then return nil end; " +
            "local p=redis.call('GET',KEYS[2]); if p then return p end; " +
            "local n=redis.call('GET',KEYS[1]); if not n or tonumber(n)<=0 then return nil end; " +
            "redis.call('SET',KEYS[2],n); redis.call('SET',KEYS[1],'0'); return n", String.class);
    private static final DefaultRedisScript<Long> ACK = new DefaultRedisScript<>(
            "if redis.call('GET',KEYS[1])==ARGV[1] then return redis.call('DEL',KEYS[2]) end; return 0", Long.class);
    private static final DefaultRedisScript<Long> UNLOCK = new DefaultRedisScript<>(
            "if redis.call('GET',KEYS[1])==ARGV[1] then return redis.call('DEL',KEYS[1]) end; return 0", Long.class);

    public void incrementViewCount(Long articleId, String visitorKey) {
        if (!shouldCount(articleId, visitorKey)) return;
        if (redisTemplate != null) {
            try {
                String key = CURRENT + articleId;
                redisTemplate.opsForValue().increment(key);
                // 滑动 TTL：文章删除或不再被访问后计数 key 自动消失，SCAN 集合不会无限增长
                redisTemplate.expire(key, Duration.ofDays(7));
                return;
            } catch (Exception e) {
                // An ambiguous Redis result may duplicate, but must not break reads.
                log.warn("Redis view increment failed; buffering locally", e);
            }
        }
        localViewCounts.merge(articleId, 1L, Long::sum);
    }

    private boolean shouldCount(Long articleId, String visitorKey) {
        String key = DEDUP + articleId + ":" + visitorKey;
        if (redisTemplate != null) {
            try {
                return Boolean.TRUE.equals(redisTemplate.opsForValue()
                        .setIfAbsent(key, "1", Duration.ofMillis(DEDUP_WINDOW_MS)));
            } catch (Exception e) {
                log.warn("Redis view dedup unavailable; using local dedup: {}", e.getMessage());
            }
        }
        long now = System.currentTimeMillis();
        if (localViewDedup.size() > 100_000) localViewDedup.clear();
        boolean[] count = {false};
        localViewDedup.compute(key, (ignored, previous) -> {
            if (previous == null || now - previous >= DEDUP_WINDOW_MS) {
                count[0] = true;
                return now;
            }
            return previous; // Do not turn a fixed 24h window into a sliding window.
        });
        return count[0];
    }

    @Scheduled(fixedRate = 300000)
    public synchronized void syncViewCountToDb() {
        // Also drain fallback counts after Redis becomes available again.
        syncFromLocal();
        if (redisTemplate == null) return;
        try {
            Set<Long> ids = new HashSet<>();
            scanArticleIds(CURRENT, ids);
            scanArticleIds(PENDING, ids);
            for (Long id : ids) syncRedisArticle(id);
        } catch (Exception e) {
            log.error("Redis view count scan failed; pending batches retained", e);
        }
    }

    private void scanArticleIds(String prefix, Set<Long> ids) {
        try (Cursor<String> cursor = redisTemplate.scan(ScanOptions.scanOptions()
                .match(prefix + "*").count(200).build())) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                try {
                    ids.add(Long.valueOf(key.substring(prefix.length())));
                } catch (NumberFormatException e) {
                    log.warn("Ignoring malformed view counter key: {}", key);
                }
            }
        }
    }

    private void syncRedisArticle(Long id) {
        String token = UUID.randomUUID().toString();
        String lockKey = LOCK + id;
        boolean locked = false;
        try {
            // Lease bounds abandoned locks. A DB call longer than this lease or
            // long JVM pause can duplicate delivery: this is not exactly-once.
            locked = Boolean.TRUE.equals(redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, token, Duration.ofMinutes(10)));
            if (!locked) return;
            String batch = redisTemplate.execute(DRAIN, List.of(CURRENT + id, PENDING + id, lockKey), token);
            if (batch == null) return;
            long count = Long.parseLong(batch);
            if (count > 0) articleMapper.incrementViewCount(id, count);
            redisTemplate.execute(ACK, List.of(lockKey, PENDING + id), token);
        } catch (Exception e) {
            // A DB exception leaves the durable pending batch intact for retry.
            log.warn("View batch not acknowledged for article {}; will retry", id, e);
        } finally {
            if (locked) {
                try {
                    redisTemplate.execute(UNLOCK, List.of(lockKey), token);
                } catch (Exception e) {
                    log.warn("View sync lock release failed; lease will expire", e);
                }
            }
        }
    }

    private void syncFromLocal() {
        for (Long id : localViewCounts.keySet()) {
            long[] drained = {0};
            // compute and merge share per-key atomicity: later increments survive.
            localViewCounts.computeIfPresent(id, (ignored, count) -> {
                drained[0] = count;
                return null;
            });
            if (drained[0] <= 0) continue;
            try {
                articleMapper.incrementViewCount(id, drained[0]);
            } catch (Exception e) {
                localViewCounts.merge(id, drained[0], Long::sum);
                log.warn("Local view batch failed for article {}; restored for retry", id, e);
            }
        }
    }
}
