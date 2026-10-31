package com.qblog.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.qblog.model.vo.ArticleVO;
import com.qblog.model.vo.CategoryVO;
import com.qblog.model.vo.TagVO;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * 热点读数据（文章详情/分类列表/标签列表）的进程内短 TTL 缓存。
 *
 * <p>设计权衡（区别于曾被移除的 Redis 快照缓存，见 {@code ArticleServiceImpl#getArticleDetail}
 * 的历史注释）：旧方案是无界快照 + 跨实例回填竞态；本方案通过以下约束把最坏陈旧窗口
 * 封顶在 TTL 上界（与 {@code CacheControlInterceptor} 允许的浏览器 2 分钟缓存同一量级）：
 * <ul>
 *   <li>只缓存已发布内容，草稿永不入缓存（避免缓存把草稿泄漏给匿名访客）；</li>
 *   <li>每条目 45~75s 随机抖动 TTL，避免批量失效后的回源风暴；</li>
 *   <li>{@link Cache#get(Object, java.util.function.Function)} 对同一 key 原子执行
 *       （single-flight），并发回源只打一次库；</li>
 *   <li>写操作通过 {@code Transactions#afterCommit} 在事务提交后失效，消除
 *       "提交前失效 → 并发回源读旧快照 → 提交后回填" 的主竞态窗口，
 *       残余窗口（回源在提交前读库、提交后安装）由 TTL 上界兜底。</li>
 * </ul>
 * 仅适用于单实例部署；若未来多实例扩容，需换用带发布/订阅失效的方案。
 */
@Component
public class ContentCacheService {

    private static final String CATEGORIES_KEY = "all";
    private static final String TAGS_KEY = "all";

    private static final long TTL_MIN_SECONDS = 45;
    private static final long TTL_JITTER_SECONDS = 30;

    private final Cache<Long, ArticleVO> articleDetailCache = Caffeine.newBuilder()
            .maximumSize(200)
            .expireAfter(new Expiry<Long, ArticleVO>() {
                @Override
                public long expireAfterCreate(Long key, ArticleVO value, long now) {
                    return jitteredTtlNanos();
                }

                @Override
                public long expireAfterUpdate(Long key, ArticleVO value, long now, long currentDurationNanos) {
                    return currentDurationNanos;
                }

                @Override
                public long expireAfterRead(Long key, ArticleVO value, long now, long currentDurationNanos) {
                    return currentDurationNanos;
                }
            })
            .build();

    private final Cache<String, List<CategoryVO>> categoryListCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(TTL_MIN_SECONDS + TTL_JITTER_SECONDS))
            .build();

    private final Cache<String, List<TagVO>> tagListCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(TTL_MIN_SECONDS + TTL_JITTER_SECONDS))
            .build();

    /**
     * 取文章详情缓存；未命中时执行 loader 并原子回填。
     */
    public ArticleVO getArticleDetail(Long id, Supplier<ArticleVO> loader) {
        return articleDetailCache.get(id, key -> loader.get());
    }

    public void evictArticle(Long id) {
        articleDetailCache.invalidate(id);
    }

    public void evictAllArticles() {
        articleDetailCache.invalidateAll();
    }

    public List<CategoryVO> getCategories(Supplier<List<CategoryVO>> loader) {
        return categoryListCache.get(CATEGORIES_KEY, key -> loader.get());
    }

    public void evictCategories() {
        categoryListCache.invalidateAll();
    }

    public List<TagVO> getTags(Supplier<List<TagVO>> loader) {
        return tagListCache.get(TAGS_KEY, key -> loader.get());
    }

    public void evictTags() {
        tagListCache.invalidateAll();
    }

    private static long jitteredTtlNanos() {
        return Duration.ofSeconds(TTL_MIN_SECONDS + ThreadLocalRandom.current().nextLong(TTL_JITTER_SECONDS + 1))
                .toNanos();
    }
}
