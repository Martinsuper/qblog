package com.qblog.ai;

import com.qblog.common.exception.BusinessException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Pattern;

/**
 * 站内智能体 web_fetch 工具的抓取实现：拉取外部网页并提取标题、摘要与正文伪 Markdown。
 * 仅 ADMIN 智能体可达（AgentTools），但 URL 来自模型/网页内容，存在被注入后探测内网的风险，
 * 故做 SSRF 护栏：限 http/https、拒绝内网与环回地址、手动跟随重定向并逐跳复检。
 */
@Component
public class WebFetchService {

    private static final int MAX_REDIRECTS = 3;
    private static final int MAX_BODY_BYTES = 2 * 1024 * 1024;
    /** 喂给模型的正文上限：过大会挤占上下文并推高 token 消耗 */
    private static final int MAX_TEXT_CHARS = 12_000;
    private static final int MAX_JSON_CHARS = 8_000;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36 QBlogAgent/1.0";

    /** 禁用主机名：环回与内网专用域名（localhost、mDNS 的 .local、保留 TLD .internal 等） */
    private static final Pattern FORBIDDEN_HOST = Pattern.compile("(?i)^([^.]+\\.)*(localhost|localdomain|local|internal)$");

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    /** 抓取 URL 并返回「标题/地址/摘要/正文」结构化文本，失败抛 BusinessException（消息可直接展示给模型） */
    public String fetch(String url) {
        URI current = sanitize(url);
        HttpResponse<InputStream> response = null;
        try {
            for (int hop = 0; ; hop++) {
                if (response != null) response.body().close();
                if (hop > MAX_REDIRECTS) throw new BusinessException("重定向次数超过 " + MAX_REDIRECTS + " 次，放弃抓取");
                response = http.send(request(current), HttpResponse.BodyHandlers.ofInputStream());
                int status = response.statusCode();
                if (status >= 300 && status < 400) {
                    String location = response.headers().firstValue("Location").orElse(null);
                    if (location == null || location.isBlank()) throw new BusinessException("目标返回 " + status + " 但缺少 Location 头");
                    current = sanitize(current.resolve(location).toString());
                    continue;
                }
                break;
            }
            int status = response.statusCode();
            if (status != 200) throw new BusinessException("抓取失败：HTTP " + status + (status == 403 ? "（该网站可能拒绝程序访问）" : ""));
            String contentType = response.headers().firstValue("Content-Type").orElse("text/html").toLowerCase();
            if (!isSupported(contentType)) throw new BusinessException("不支持的内容类型：" + contentType.split(";")[0].trim());
            return render(current, contentType, response.body());
        } catch (IOException e) {
            String reason = e.getMessage() == null ? "网络错误" : e.getMessage();
            throw new BusinessException("抓取失败：" + reason, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("抓取被中断", e);
        } finally {
            if (response != null) {
                try { response.body().close(); } catch (IOException ignored) { }
            }
        }
    }

    private HttpRequest request(URI uri) {
        return HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/json,text/plain,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .GET()
                .build();
    }

    /** 逐跳安全校验：scheme 白名单 + 主机名黑名单 + 解析出的 IP 不落内网/环回 */
    private URI sanitize(String url) {
        URI uri;
        try {
            uri = URI.create(url == null ? "" : url.trim()).normalize();
        } catch (IllegalArgumentException e) {
            throw new BusinessException("无效网址：" + url);
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (!scheme.equals("http") && !scheme.equals("https")) throw new BusinessException("仅支持 http/https 网址");
        String host = uri.getHost();
        if (host == null || host.isBlank()) throw new BusinessException("网址缺少主机名：" + url);
        if (FORBIDDEN_HOST.matcher(host).matches()) throw new BusinessException("禁止访问内网主机：" + host);
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) requirePublic(address);
        } catch (IOException e) {
            throw new BusinessException("域名解析失败：" + host);
        }
        return uri;
    }

    private void requirePublic(InetAddress address) {
        // isSiteLocal 覆盖 10/8、172.16/12、192.168/16；isLinkLocal 覆盖 169.254/16（含云元数据）与 fe80::/10。
        // fc00::/7（IPv6 ULA，局域网自配段）Java 未归入 site-local，需单独判断。
        // 198.18.0.0/15（基准测试保留段）不拦：fake-IP 代理环境（本机即此场景）会把所有域名解析到该段。
        if (address.isLoopbackAddress() || address.isAnyLocalAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()
                || isUniqueLocalV6(address))
            throw new BusinessException("禁止访问内网或保留地址：" + address.getHostAddress());
    }

    private boolean isUniqueLocalV6(InetAddress address) {
        byte[] bytes = address.getAddress();
        return bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
    }

    private boolean isSupported(String contentType) {
        return contentType.startsWith("text/")
                || contentType.contains("html")
                || contentType.contains("json")
                || contentType.contains("xml");
    }

    /** 包级可见以便单测直接验证提取逻辑（不起网络） */
    String render(URI url, String contentType, InputStream body) throws IOException {
        byte[] bytes = readCapped(body);
        if (contentType.contains("json") || contentType.contains("xml") || contentType.startsWith("text/plain")) {
            return truncated("【地址】" + url + "\n【原始内容】\n" + new String(bytes, StandardCharsets.UTF_8), MAX_JSON_CHARS);
        }
        // 先限量读入内存再交给 Jsoup，避免超大 HTML 无上限占用内存；null 让 Jsoup 依 meta/响应头探测字符集
        Document doc = Jsoup.parse(new java.io.ByteArrayInputStream(bytes), null, url.toString());
        doc.select("script,style,noscript,iframe,svg,form,button,nav,header,footer,aside").remove();
        String title = doc.title();
        if (title == null || title.isBlank()) title = meta(doc, "og:title");
        String description = meta(doc, "description");
        if (description == null || description.isBlank()) description = meta(doc, "og:description");
        Element main = doc.selectFirst("article, main, [role=main], #content, .content");
        String text = toMarkdown(main != null ? main : doc.body());
        if (main != null && text.length() < 200) text = toMarkdown(doc.body()); // 主内容选择器命中过少时退回整页
        if (text.isBlank()) throw new BusinessException("页面没有可提取的正文（可能是纯脚本渲染页）");
        return "【标题】" + title + "\n【地址】" + url
                + "\n【摘要】" + (description == null ? "" : description)
                + "\n【正文】\n" + truncated(text, MAX_TEXT_CHARS);
    }

    private String meta(Document doc, String key) {
        Element node = doc.selectFirst("meta[name=" + key + "], meta[property=" + key + "]");
        return node == null ? null : node.attr("content");
    }

    private byte[] readCapped(InputStream body) throws IOException {
        byte[] buffer = new byte[8192];
        int total = 0, read;
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        while ((read = body.read(buffer)) != -1) {
            total += read;
            if (total > MAX_BODY_BYTES) throw new BusinessException("响应体超过 " + (MAX_BODY_BYTES / 1024 / 1024) + "MB 上限");
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    /** 主内容 → 伪 Markdown：标题映射 #、列表/代码/引用保留结构，链接仅保留纯 ASCII 目标（与笔记链接规范一致） */
    private String toMarkdown(Element root) {
        StringBuilder out = new StringBuilder();
        appendNode(root, out);
        return out.toString().replaceAll("\n{3,}", "\n\n").strip();
    }

    /** 按 DOM 节点遍历（而非仅子元素）：容器自身的直接文本（深层 div 链常见）不会丢失 */
    private void appendNode(Element element, StringBuilder out) {
        for (org.jsoup.nodes.Node node : element.childNodes()) {
            if (node instanceof org.jsoup.nodes.TextNode text) {
                String t = text.text().replaceAll("\\s+", " ").strip();
                if (!t.isEmpty()) out.append('\n').append(t).append('\n');
            } else if (node instanceof Element child) {
                appendElement(child, out);
            }
        }
    }

    private void appendElement(Element child, StringBuilder out) {
        switch (child.tagName()) {
            case "h1", "h2" -> out.append("\n## ").append(child.text()).append('\n');
            case "h3" -> out.append("\n### ").append(child.text()).append('\n');
            case "h4", "h5", "h6" -> out.append("\n#### ").append(child.text()).append('\n');
            case "p" -> out.append('\n').append(inline(child)).append('\n');
            case "li" -> out.append("\n- ").append(inline(child)).append('\n');
            case "pre" -> out.append("\n```\n").append(child.text()).append("\n```\n");
            case "blockquote" -> {
                for (String line : child.text().split("\n")) out.append("> ").append(line).append('\n');
            }
            case "br", "hr" -> out.append('\n');
            case "ul", "ol", "dl", "dt", "dd", "table", "tbody", "thead", "tr", "div", "section", "article", "figure", "figcaption" -> appendNode(child, out);
            default -> {
                String own = inline(child);
                if (!own.isBlank()) out.append('\n').append(own).append('\n');
            }
        }
    }

    private String inline(Element element) {
        StringBuilder out = new StringBuilder();
        elementNode(element, out);
        return out.toString().replaceAll("\\s+", " ").strip();
    }

    private void elementNode(Element element, StringBuilder out) {
        for (org.jsoup.nodes.Node node : element.childNodes()) {
            if (node instanceof org.jsoup.nodes.TextNode text) {
                out.append(text.text());
            } else if (node instanceof Element child) {
                if (child.tagName().equals("a")) {
                    String href = child.attr("abs:href");
                    String label = child.text().strip();
                    if (!href.isBlank() && href.matches("^[\\x21-\\x7e]+$")) {
                        // 与 NoteMarkdownPolicy 一致：仅输出无空格/中文/全角字符的 ASCII 链接
                        out.append('[').append(label).append("](").append(href).append(')');
                        continue;
                    }
                    out.append(label);
                } else {
                    elementNode(child, out);
                }
            }
        }
    }

    private String truncated(String text, int limit) {
        if (text.length() <= limit) return text;
        return text.substring(0, limit) + "\n（内容过长已截断，如需更多请让用户给出更具体的子页面地址）";
    }
}
