package com.qblog.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * PlantUML 进程内渲染：源码字符串 → SVG 字节。
 * 进程内缓存渲染结果；渲染串行限流（形进程内无队列膨胀），带超时，防止恶意源码占用 Java 线程。
 * 若本机缺 Graphviz 而渲染失败，追加 !pragma layout smetana 后重试一次（纯 Java 布局）。
 */
@Slf4j
@Service
public class PlantUmlRenderService {

    private static final int MAX_RENDER_CONCURRENCY = 2;
    private static final long RENDER_TIMEOUT_SECONDS = 20;

    private final ExecutorService worker = Executors.newFixedThreadPool(MAX_RENDER_CONCURRENCY, r -> {
        Thread t = new Thread(r, "plantuml-render");
        t.setDaemon(true);
        return t;
    });
    private final Semaphore permits = new Semaphore(MAX_RENDER_CONCURRENCY);
    private final Cache<String, byte[]> cache = Caffeine.newBuilder()
            .maximumWeight(24L * 1024 * 1024)
            .weigher((String k, byte[] v) -> Math.max(1, v.length))
            .expireAfterAccess(Duration.ofHours(12))
            .build();

    /** 渲染失败时返回内嵌错误提示的 SVG，而不是异常，保证 <img> 总能显示。 */
    public byte[] render(String source) {
        return cache.get(cacheKey(source), key -> renderWithFallback(source));
    }

    private byte[] renderWithFallback(String source) {
        try {
            return renderOnce(source);
        } catch (Exception plain) {
            log.debug("PlantUML 原样渲染失败，尝试 Smetana 布局：{}", plain.getMessage());
            if (source.startsWith("@start") || source.startsWith("@end")) {
                String pragma = "!pragma layout smetana\n";
                String body = source.startsWith("@start") ? pragma + source : source + "\n" + pragma;
                try {
                    return renderOnce(body);
                } catch (Exception smetana) {
                    log.info("PlantUML Smetana 渲染也失败：{}", smetana.getMessage());
                }
            }
            return errorSvg(plain);
        }
    }

    private byte[] renderOnce(String source) throws Exception {
        permits.acquire();
        Future<byte[]> future = worker.submit((Callable<byte[]>) () -> {
            SourceStringReader reader = new SourceStringReader(source);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            reader.outputImage(out, new FileFormatOption(FileFormat.SVG, false));
            byte[] svg = out.toByteArray();
            // Graphviz 缺失时 PlantUML 会生成带错误文本的 SVG，而非抛异常
            if (containsErrorText(svg)) throw new IllegalStateException("Graphviz/dot 渲染失败（已在缺失时回退 Smetana）");
            return svg;
        });
        try {
            return future.get(RENDER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new IllegalStateException("渲染超时");
        } finally {
            permits.release();
        }
    }

    /** PlantUML 的错误 SVG 故意包含 "Syntax Error / Graphviz" 文案，用来探测。 */
    private boolean containsErrorText(byte[] svg) {
        String text = new String(svg, StandardCharsets.UTF_8);
        return text.contains("SyntaxError") || text.contains("Syntax Error") || text.contains("Graphviz");
    }

    private String cacheKey(String source) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private byte[] errorSvg(Exception e) {
        String message = e.getMessage() == null ? e.getClass().getSimpleName()
                : e.getMessage().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        if (message.length() > 200) message = message.substring(0, 200);
        return ("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"520\" height=\"72\">"
                + "<rect width=\"100%\" height=\"100%\" fill=\"#fef0f0\" stroke=\"#fde2e2\"/>"
                + "<text x=\"50%\" y=\"50%\" text-anchor=\"middle\" dominant-baseline=\"middle\" "
                + "fill=\"#f56c6c\" font-family=\"-apple-system,sans-serif\" font-size=\"13\">"
                + "PlantUML 渲染失败：" + message
                + "</text></svg>").getBytes(StandardCharsets.UTF_8);
    }
}
