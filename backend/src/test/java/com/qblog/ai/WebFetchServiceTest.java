package com.qblog.ai;

import com.qblog.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class WebFetchServiceTest {

    private final WebFetchService service = new WebFetchService();

    // ---------------------------------------------------------------- URL 护栏（不发网络请求，校验在解析/解析 IP 阶段即抛出）

    @Test void rejectsNonHttpSchemes() {
        assertThrows(BusinessException.class, () -> service.fetch("ftp://example.com/file"));
        assertThrows(BusinessException.class, () -> service.fetch("file:///etc/passwd"));
        assertThrows(BusinessException.class, () -> service.fetch("javascript:alert(1)"));
    }

    @Test void rejectsLoopbackAndPrivateHosts() {
        assertThrows(BusinessException.class, () -> service.fetch("http://localhost:8080/admin"));
        assertThrows(BusinessException.class, () -> service.fetch("http://127.0.0.1:8080/"));
        assertThrows(BusinessException.class, () -> service.fetch("http://10.1.2.3/"));
        assertThrows(BusinessException.class, () -> service.fetch("http://192.168.1.1/"));
        assertThrows(BusinessException.class, () -> service.fetch("http://172.16.0.9/"));
        assertThrows(BusinessException.class, () -> service.fetch("http://169.254.169.254/latest/meta-data/"));
        assertThrows(BusinessException.class, () -> service.fetch("http://[::1]:8080/"));
        assertThrows(BusinessException.class, () -> service.fetch("http://[fd00::1234]/"));
    }

    @Test void rejectsMissingHost() {
        assertThrows(BusinessException.class, () -> service.fetch("http:///path"));
        assertThrows(BusinessException.class, () -> service.fetch(null));
    }

    // ---------------------------------------------------------------- HTML 提取（直接喂字节流，不起网络）

    private static final String PAGE = """
            <html><head>
              <title>Spring AI 工具调用指南</title>
              <meta name="description" content="介绍 Spring AI 的工具调用机制。">
            </head><body>
              <nav>导航 导航 导航</nav>
              <article>
                <h1>Spring AI 工具调用指南</h1>
                <p>工具调用让模型可以 <a href="https://docs.example.com/tools">查阅文档</a>，
                   也可以访问 <a href="/relative/path">站内页面</a>。</p>
                <h2>一、基本流程</h2>
                <p>模型返回 tool_calls 后框架自动执行。</p>
                <ul><li>注册工具</li><li>执行并回传结果</li></ul>
                <pre><code>client.prompt().tools(new AgentTools())</code></pre>
                <blockquote>引用内容</blockquote>
              </article>
              <footer>页脚 页脚</footer>
            </body></html>
            """;

    private String renderPage() {
        try {
            return service.render(URI.create("https://example.com/blog/spring-ai-tools"),
                    "text/html; charset=utf-8", new ByteArrayInputStream(PAGE.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test void extractsTitleDescriptionAndMainContent() {
        String out = renderPage();
        assertTrue(out.startsWith("【标题】Spring AI 工具调用指南"));
        assertTrue(out.contains("【摘要】介绍 Spring AI 的工具调用机制。"));
        assertTrue(out.contains("## 一、基本流程"));
        assertTrue(out.contains("- 注册工具"));
        assertTrue(out.contains("```\nclient.prompt().tools(new AgentTools())\n```"));
        assertTrue(out.contains("> 引用内容"));
    }

    @Test void dropsChromeAndKeepsAsciiLinksOnly() {
        String out = renderPage();
        assertFalse(out.contains("导航"), "应剔除 nav 区块");
        assertFalse(out.contains("页脚"), "应剔除 footer 区块");
        assertTrue(out.contains("[查阅文档](https://docs.example.com/tools)"), "纯 ASCII 链接保留");
        assertTrue(out.contains("[站内页面](https://example.com/relative/path)"), "相对链接按 base 补全为绝对地址后保留");
    }

    @Test void truncatesLongContent() {
        StringBuilder huge = new StringBuilder("<html><head><title>t</title></head><body><article>");
        for (int i = 0; i < 2000; i++) huge.append("<p>第").append(i).append("段，这是一段足够长的测试正文内容。</p>");
        huge.append("</article></body></html>");
        String out;
        try {
            out = service.render(URI.create("https://example.com/huge"), "text/html",
                    new ByteArrayInputStream(huge.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        assertTrue(out.contains("已截断"));
    }

    @Test void passesThroughPlainText() {
        String out;
        try {
            out = service.render(URI.create("https://example.com/data.json"), "application/json",
                    new ByteArrayInputStream("{\"a\":1}".getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        assertTrue(out.contains("【原始内容】"));
        assertTrue(out.contains("{\"a\":1}"));
    }

    // ---------------------------------------------------------------- externalId 派生

    @Test void deriveExternalIdIsDeterministicAndDistinct() {
        String a1 = AgentTools.deriveExternalId("Spring AI 入门");
        String a2 = AgentTools.deriveExternalId("Spring AI 入门 ");
        String b = AgentTools.deriveExternalId("React 入门");
        assertEquals(a1, a2, "同题（含首尾空白差异）应派生同一幂等键");
        assertNotEquals(a1, b);
        assertTrue(a1.startsWith("agent-"));
        assertTrue(a1.matches("agent-[0-9a-f]{32}"), "ASCII 安全，满足 sourceTool/externalId 校验");
        assertTrue(a1.length() <= 200);
    }
}
